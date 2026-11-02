package dev.nullcode.mediamagnet.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.ContextCompat
import dev.nullcode.mediamagnet.data.ApiClient
import dev.nullcode.mediamagnet.data.ApiException
import dev.nullcode.mediamagnet.data.AppPrefs
import dev.nullcode.mediamagnet.data.AppPreferences
import dev.nullcode.mediamagnet.data.CookieInput
import dev.nullcode.mediamagnet.data.DownloadRecord
import dev.nullcode.mediamagnet.data.DownloadRequest
import dev.nullcode.mediamagnet.data.FileSaver
import dev.nullcode.mediamagnet.data.Health
import dev.nullcode.mediamagnet.data.JobCookie
import dev.nullcode.mediamagnet.data.JobSummary
import dev.nullcode.mediamagnet.data.ServerSettings
import dev.nullcode.mediamagnet.data.ToolList
import dev.nullcode.mediamagnet.data.ToolUpdate
import dev.nullcode.mediamagnet.data.TransportEvent
import dev.nullcode.mediamagnet.service.DownloadService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.util.Collections
import java.util.UUID

class MediaMagnetEngine private constructor(context: Context) {
    private val app = context.applicationContext
    private val store = AppPreferences(app)
    private val saver = FileSaver(app)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var client: ApiClient? = null
    private var streamJob: Job? = null
    private var idleStopJob: Job? = null
    private val savedOutputs: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private val progressMarks = mutableMapOf<String, Long>()
    private val transferMarks = mutableMapOf<String, Long>()

    private val _prefs = MutableStateFlow(AppPrefs())
    val prefs: StateFlow<AppPrefs> = _prefs.asStateFlow()

    private val _connection = MutableStateFlow<Connection>(Connection.Idle)
    val connection: StateFlow<Connection> = _connection.asStateFlow()

    private val _health = MutableStateFlow<Health?>(null)
    val health: StateFlow<Health?> = _health.asStateFlow()

    private val _jobs = MutableStateFlow<Map<String, JobUi>>(emptyMap())
    val jobs: StateFlow<Map<String, JobUi>> = _jobs.asStateFlow()

    private val _settings = MutableStateFlow(ServerSettings())
    val settings: StateFlow<ServerSettings> = _settings.asStateFlow()

    private val _cookies = MutableStateFlow<Map<String, String>>(emptyMap())
    val cookies: StateFlow<Map<String, String>> = _cookies.asStateFlow()

    private val _serverInfo = MutableStateFlow(ServerInfo())
    val serverInfo: StateFlow<ServerInfo> = _serverInfo.asStateFlow()

    private val _tools = MutableStateFlow<ToolList?>(null)
    val tools: StateFlow<ToolList?> = _tools.asStateFlow()

    private val _toolsUpdating = MutableStateFlow(false)
    val toolsUpdating: StateFlow<Boolean> = _toolsUpdating.asStateFlow()

    private val _toolUpdate = MutableStateFlow<ToolUpdate?>(null)
    val toolUpdate: StateFlow<ToolUpdate?> = _toolUpdate.asStateFlow()

    private val _transfers = MutableStateFlow<Map<String, TransferUi>>(emptyMap())
    val transfers: StateFlow<Map<String, TransferUi>> = _transfers.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 16)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init {
        scope.launch {
            cleanupPendingWrites()
            store.flow.collect(::applyPrefs)
        }
        scope.launch {
            combine(_jobs, _transfers) { jobs, transfers ->
                jobs.values.any { it.isRunning || it.isPaused } || transfers.isNotEmpty()
            }.distinctUntilChanged().collect(::syncService)
        }
    }

    private suspend fun cleanupPendingWrites() {
        store.takePendingUris().forEach { text ->
            val uri = Uri.parse(text)
            runCatching {
                if (uri.scheme == "file") {
                    uri.path?.let { File(it).delete() }
                } else {
                    app.contentResolver.delete(uri, null, null)
                }
            }
        }
    }

    private fun syncService(active: Boolean) {
        val intent = Intent(app, DownloadService::class.java)
        if (active) {
            idleStopJob?.cancel()
            idleStopJob = null
            runCatching { ContextCompat.startForegroundService(app, intent) }
        } else if (idleStopJob == null) {
            idleStopJob = scope.launch {
                delay(IDLE_STOP_DELAY_MS)
                app.stopService(intent)
                idleStopJob = null
            }
        }
    }

    private fun applyPrefs(value: AppPrefs) {
        _prefs.value = value
        _settings.value = value.settings
        val next = ApiClient.create(value.baseUrl, value.token)
        if (next?.key == client?.key) return

        client = next
        streamJob?.cancel()
        _health.value = null
        _tools.value = null
        _toolUpdate.value = null
        _cookies.value = emptyMap()
        _serverInfo.value = ServerInfo()
        _jobs.value = emptyMap()

        if (next == null) {
            _connection.value = Connection.Idle
        } else {
            connect(next)
        }
    }

    private fun connect(target: ApiClient) {
        streamJob = scope.launch {
            var failures = 0
            while (isActive && client === target) {
                try {
                    val health = target.health()
                    _health.value = health
                    _connection.value = Connection.Online(health.version)
                    failures = 0
                    refreshJobs(target)
                    target.events().collect(::onEvent)
                    delay(1_000)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    failures++
                    _connection.value = Connection.Offline(error.message ?: "Connection failed")
                    delay(minOf(30_000L, 1_000L * failures))
                }
            }
        }
    }

    private fun onEvent(event: TransportEvent) {
        when (event) {
            is TransportEvent.Started -> updateJob(event.id) { copy(state = "running", error = null) }

            is TransportEvent.Progress -> {
                val now = SystemClock.elapsedRealtime()
                if (now - (progressMarks[event.id] ?: 0) < PROGRESS_INTERVAL_MS) return
                progressMarks[event.id] = now
                updateJob(event.id) {
                    copy(progress = event.value.toFloat().coerceIn(0f, 1f), state = "running")
                }
            }

            is TransportEvent.Status -> updateJob(event.id) { copy(status = event.value) }

            is TransportEvent.State -> {
                if (event.value == "finished") {
                    progressMarks.remove(event.id)
                    _jobs.update { it - event.id }
                    scope.launch { refreshJob(event.id) }
                } else {
                    updateJob(event.id) {
                        copy(
                            state = event.value,
                            error = if (event.value == "failed") this.error else null,
                        )
                    }
                }
            }

            is TransportEvent.Error -> {
                updateJob(event.id) { copy(state = "failed", error = event.value) }
                emit(event.value, isError = true)
            }

            is TransportEvent.Finished -> {
                val job = _jobs.value[event.id]?.copy(
                    state = "finished",
                    files = event.files,
                    progress = 1f,
                ) ?: JobUi(
                    id = event.id,
                    url = "",
                    state = "finished",
                    files = event.files,
                    createdAtMs = 0,
                )
                progressMarks.remove(event.id)
                _jobs.update { it - event.id }
                autoSave(job)
            }

            is TransportEvent.Notification -> emit(event.message, isError = event.level == "error")
        }
    }

    private fun updateJob(id: String, transform: JobUi.() -> JobUi) {
        _jobs.update { jobs -> jobs[id]?.let { jobs + (id to it.transform()) } ?: jobs }
    }

    private suspend fun refreshJobs(target: ApiClient) {
        val list = runCatching { target.listDownloads() }.getOrElse { return }
        mergeJobs(list)
    }

    private suspend fun refreshJob(id: String) {
        val target = client ?: return
        val summary = runCatching { target.getDownload(id) }.getOrNull() ?: return
        mergeJob(summary)
    }

    private fun mergeJobs(list: List<JobSummary>) {
        val previous = _jobs.value
        val active = mutableMapOf<String, JobUi>()
        list.forEach { summary ->
            val job = summary.toUi(previous[summary.id])
            if (summary.state == "finished") autoSave(job) else active[summary.id] = job
        }
        _jobs.value = active
    }

    private fun mergeJob(summary: JobSummary) {
        val job = summary.toUi(_jobs.value[summary.id])
        if (summary.state == "finished") {
            _jobs.update { it - summary.id }
            autoSave(job)
        } else {
            _jobs.update { it + (summary.id to job) }
        }
    }

    private fun JobSummary.toUi(old: JobUi?) = JobUi(
        id = id,
        url = url,
        state = state,
        files = files,
        createdAtMs = createdAtMs,
        progress = if (state == "finished") 1f else old?.progress,
        status = old?.status,
        error = if (state == "failed") old?.error else null,
    )

    private fun autoSave(job: JobUi) {
        if (!job.isFinished) return
        val paths = job.files.ifEmpty { listOf("") }
        for (path in paths) {
            val key = "${job.id}|$path"
            if (!savedOutputs.add(key)) continue
            saveJobOutput(job, path, key)
        }
    }

    private fun saveJobOutput(job: JobUi, path: String, savedKey: String) {
        val target = client ?: return
        val id = job.id
        val name = path.substringAfterLast('/').ifBlank { "download-$id" }
        val transferKey = "job:$savedKey"
        scope.launch {
            _transfers.update {
                it + (transferKey to TransferUi(transferKey, name, 0, null, null))
            }
            var created: Uri? = null
            try {
                val savedPath = path.ifBlank { name }
                val uri = saver.save(
                    savedPath,
                    FileSaver.guessMime(name),
                    _prefs.value.treeUri,
                    onCreated = { created = it; store.addPendingUri(it.toString()) },
                ) { sink ->
                    target.downloadJobFile(id, path.ifBlank { null }, sink) { done, total ->
                        updateTransfer(transferKey, done, total)
                    }
                }
                store.removePendingUri(uri.toString())
                recordDownload(job, savedPath, uri)
                transferMarks.remove(transferKey)
                _transfers.update { it - transferKey }
                emit("Saved $name", isError = false)
            } catch (cancelled: CancellationException) {
                created?.let { dropPending(it) }
                savedOutputs.remove(savedKey)
                transferMarks.remove(transferKey)
                _transfers.update { it - transferKey }
                throw cancelled
            } catch (error: Exception) {
                created?.let { dropPending(it) }
                transferMarks.remove(transferKey)
                _transfers.update { it - transferKey }
                if (error is ApiException && error.status == 404) return@launch
                savedOutputs.remove(savedKey)
                emit("Could not save $name: ${error.describe()}", isError = true)
            }
        }
    }

    private suspend fun dropPending(uri: Uri) {
        withContext(NonCancellable) { store.removePendingUri(uri.toString()) }
    }

    private suspend fun recordDownload(job: JobUi, path: String, uri: Uri) {
        val name = path.substringAfterLast('/').ifBlank { "download" }
        store.addHistory(
            DownloadRecord(
                id = UUID.randomUUID().toString(),
                jobId = job.id,
                url = job.url,
                name = name,
                path = path,
                uri = uri.toString(),
                savedAtMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun updateTransfer(key: String, done: Long, total: Long?) {
        val now = SystemClock.elapsedRealtime()
        if (now - (transferMarks[key] ?: 0) < PROGRESS_INTERVAL_MS) return
        transferMarks[key] = now
        _transfers.update { current ->
            val transfer = current[key] ?: return@update current
            current + (key to transfer.copy(
                received = done,
                total = total,
                fraction = total?.takeIf { it > 0 }?.let { (done.toFloat() / it).coerceIn(0f, 1f) },
            ))
        }
    }

    fun startDownload(
        url: String,
        backend: String?,
        overridesJson: String,
        cookieDomain: String,
        cookieContent: String,
    ) {
        val target = client ?: return emit("Set the server address first", isError = true)
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return emit("Enter a URL", isError = true)

        val overrides: JsonObject? = overridesJson.trim().takeIf { it.isNotEmpty() }?.let { text ->
            val parsed = runCatching { json.parseToJsonElement(text) }.getOrNull()
            if (parsed !is JsonObject) {
                emit("Overrides must be a JSON object", isError = true)
                return
            }
            parsed
        }

        val cookies = if (cookieDomain.isNotBlank() && cookieContent.isNotBlank()) {
            listOf(JobCookie(cookieDomain.trim(), cookieContent))
        } else {
            null
        }

        val local = json.encodeToJsonElement(_prefs.value.settings).jsonObject
        val merged = JsonObject(local - "settings_version" + overrides.orEmpty())

        scope.launch {
            try {
                val accepted = target.startDownload(
                    DownloadRequest(
                        url = trimmed,
                        backend = backend,
                        overrides = merged,
                        cookies = cookies,
                    ),
                )
                _jobs.update { jobs ->
                    jobs + (accepted.id to JobUi(
                        id = accepted.id,
                        url = trimmed,
                        state = "running",
                        files = emptyList(),
                        createdAtMs = System.currentTimeMillis(),
                    ))
                }
                emit("Download started", isError = false)
            } catch (error: Exception) {
                emit(error.describe(), isError = true)
            }
        }
    }

    fun pause(id: String) = act("Could not pause") { it.pause(id) }
    fun resume(id: String) = act("Could not resume") { it.resume(id).id }
    fun cancel(id: String) = act("Could not cancel") { it.cancel(id) }
    fun cancelAll() = act("Could not cancel downloads") { it.cancelAll() }

    fun removeHistory(jobId: String) = scope.launch { store.removeHistory(jobId) }

    fun refresh() {
        val target = client ?: return emit("Set the server address first", isError = true)
        scope.launch {
            try {
                val health = target.health()
                _health.value = health
                _connection.value = Connection.Online(health.version)
                refreshJobs(target)
                _cookies.value = target.listCookies()
                _tools.value = target.listTools()
                refreshPaths(target)
            } catch (error: Exception) {
                _connection.value = Connection.Offline(error.describe())
                emit(error.describe(), isError = true)
            }
        }
    }

    fun saveSettings(settings: ServerSettings) {
        _settings.value = settings
        scope.launch {
            store.setSettings(settings)
            emit("Settings saved", isError = false)
        }
    }

    fun resetSettings() {
        val defaults = ServerSettings()
        _settings.value = defaults
        scope.launch {
            store.setSettings(defaults)
            emit("Settings reset", isError = false)
        }
    }

    fun loadCookies() = act("Could not load cookies") { _cookies.value = it.listCookies() }

    fun saveCookie(domain: String, type: String, value: String) {
        if (domain.isBlank()) return emit("Enter a domain", isError = true)
        if (value.isBlank()) return emit("Enter cookie content or a file path", isError = true)
        act("Could not save cookie") { target ->
            val saved = target.saveCookie(domain.trim(), CookieInput(type, value))
            _cookies.update { it + (saved.domain to saved.path) }
            emit("Cookie saved for ${saved.domain}", isError = false)
        }
    }

    fun deleteCookie(domain: String) = act("Could not delete cookie") {
        it.deleteCookie(domain)
        _cookies.update { cookies -> cookies - domain }
    }

    fun clearCookies() = act("Could not clear cookies") {
        val result = it.clearCookies()
        _cookies.value = emptyMap()
        emit("Deleted ${result.deleted} cookie profiles", isError = false)
    }

    fun loadTools() = act("Could not load tools") { _tools.value = it.listTools() }

    fun updateTools() {
        val target = client ?: return emit("Set the server address first", isError = true)
        if (_toolsUpdating.value) return
        _toolsUpdating.value = true
        scope.launch {
            try {
                _toolUpdate.value = target.updateTools()
                _tools.value = target.listTools()
            } catch (error: Exception) {
                emit(error.describe(), isError = true)
            } finally {
                _toolsUpdating.value = false
            }
        }
    }

    fun dismissToolUpdate() {
        _toolUpdate.value = null
    }

    fun refreshPaths() {
        val target = client ?: return emit("Set the server address first", isError = true)
        scope.launch {
            try {
                refreshPaths(target)
            } catch (error: Exception) {
                emit(error.describe(), isError = true)
            }
        }
    }

    private suspend fun refreshPaths(target: ApiClient) {
        _serverInfo.value = ServerInfo(
            root = target.downloadRoot().path,
            usedPercent = target.diskUsage().usedPercent,
        )
    }

    fun setBaseUrl(value: String) = scope.launch { store.setBaseUrl(value) }
    fun setToken(value: String) = scope.launch { store.setToken(value) }
    fun setTreeUri(value: String?) = scope.launch { store.setTreeUri(value) }

    fun emit(text: String, isError: Boolean) {
        if (text.isBlank()) return
        _messages.tryEmit(UiMessage(text, isError))
    }

    private fun act(label: String, block: suspend (ApiClient) -> Any?) {
        val target = client ?: return emit("Set the server address first", isError = true)
        scope.launch {
            try {
                block(target)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                emit("$label: ${error.describe()}", isError = true)
            }
        }
    }

    companion object {
        private const val IDLE_STOP_DELAY_MS = 15_000L
        private const val PROGRESS_INTERVAL_MS = 100L

        @Volatile
        private var instance: MediaMagnetEngine? = null

        fun get(context: Context): MediaMagnetEngine =
            instance ?: synchronized(this) {
                instance ?: MediaMagnetEngine(context).also { instance = it }
            }
    }
}

private fun Throwable.describe(): String = when (this) {
    is ApiException -> if (code == "http_$status") message ?: "HTTP $status" else "$code — $message"
    else -> message ?: javaClass.simpleName
}
