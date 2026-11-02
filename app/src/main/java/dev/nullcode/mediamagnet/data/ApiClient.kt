package dev.nullcode.mediamagnet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class ApiClient private constructor(
    private val root: HttpUrl,
    private val token: String,
) {
    val key: Pair<String, String> get() = root.toString() to token

    private val apiBase: HttpUrl =
        if (root.encodedPath.trimEnd('/').endsWith("/api/v1")) {
            root
        } else {
            root.newBuilder().addPathSegments("api/v1").build()
        }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val streamClient = client.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private fun builder(vararg segments: String): HttpUrl.Builder =
        apiBase.newBuilder().apply { segments.forEach(::addPathSegment) }

    private fun Request.Builder.auth(): Request.Builder =
        apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }

    private suspend fun call(
        url: HttpUrl,
        method: String = "GET",
        body: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).auth().apply {
            when (method) {
                "DELETE" -> delete()
                "POST" -> post((body ?: "{}").toRequestBody(JSON))
                "PUT" -> put((body ?: "{}").toRequestBody(JSON))
                else -> get()
            }
        }.build()

        client.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) throw error(response.code, text)
            text
        }
    }

    private fun error(status: Int, body: String): ApiException {
        val parsed = runCatching { json.decodeFromString<ApiErrorBody>(body) }.getOrNull()
        val files = runCatching { json.decodeFromString<MultipleFilesBody>(body).files }
            .getOrDefault(emptyList())
        return ApiException(
            status = status,
            code = parsed?.error ?: "http_$status",
            message = parsed?.message ?: body.take(400).ifBlank { "HTTP $status" },
            files = files,
        )
    }

    suspend fun health(): Health =
        json.decodeFromString(call(builder("health").build()))

    suspend fun startDownload(request: DownloadRequest): DownloadAccepted =
        json.decodeFromString(call(builder("downloads").build(), "POST", json.encodeToString(request)))

    suspend fun listDownloads(): List<JobSummary> =
        json.decodeFromString<JobList>(call(builder("downloads").build())).jobs

    suspend fun getDownload(id: String): JobSummary =
        json.decodeFromString(call(builder("downloads", id).build()))

    suspend fun pause(id: String) {
        call(builder("downloads", id, "pause").build(), "POST")
    }

    suspend fun resume(id: String): DownloadAccepted =
        json.decodeFromString(call(builder("downloads", id, "resume").build(), "POST"))

    suspend fun cancel(id: String) {
        call(builder("downloads", id).build(), "DELETE")
    }

    suspend fun cancelAll() {
        call(builder("downloads").build(), "DELETE")
    }

    suspend fun getSettings(): ServerSettings =
        json.decodeFromString(call(builder("settings").build()))

    suspend fun updateSettings(settings: ServerSettings): ServerSettings =
        json.decodeFromString(call(builder("settings").build(), "PUT", json.encodeToString(settings)))

    suspend fun resetSettings(): ServerSettings =
        json.decodeFromString(call(builder("settings", "reset").build(), "POST"))

    suspend fun listCookies(): Map<String, String> =
        json.decodeFromString(call(builder("cookies").build()))

    suspend fun saveCookie(domain: String, input: CookieInput): SavedCookie =
        json.decodeFromString(
            call(builder("cookies").build(), "POST", json.encodeToString(SaveCookieRequest(domain, input))),
        )

    suspend fun deleteCookie(domain: String) {
        call(builder("cookies", domain).build(), "DELETE")
    }

    suspend fun clearCookies(): DeletedCount =
        json.decodeFromString(call(builder("cookies").build(), "DELETE"))

    suspend fun downloadRoot(): DownloadRoot =
        json.decodeFromString(call(builder("paths", "download-root").build()))

    suspend fun diskUsage(): DiskUsage =
        json.decodeFromString(call(builder("paths", "disk-usage").build()))

    suspend fun listTools(): ToolList =
        json.decodeFromString(call(builder("tools").build()))

    suspend fun updateTools(): ToolUpdate =
        json.decodeFromString(call(builder("tools", "update").build(), "POST"))

    fun events(): Flow<TransportEvent> = flow {
        val request = Request.Builder()
            .url(builder("events").build())
            .auth()
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        streamClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw error(response.code, response.body.string())
            }
            val reader = response.body.byteStream().bufferedReader()
            val data = StringBuilder()
            while (true) {
                currentCoroutineContext().ensureActive()
                val line = reader.readLine() ?: break
                when {
                    line.isEmpty() -> {
                        if (data.isNotEmpty()) {
                            parseEvent(data.toString())?.let { emit(it) }
                            data.setLength(0)
                        }
                    }
                    line.startsWith("data:") -> {
                        data.append(line.removePrefix("data:").trimStart())
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun downloadJobFile(
        id: String,
        path: String?,
        sink: OutputStream,
        onProgress: (Long, Long?) -> Unit,
    ) {
        val url = builder("downloads", id, "file")
            .apply { if (path != null) addQueryParameter("path", path) }
            .build()
        stream(Request.Builder().url(url).auth().build(), sink, onProgress)
    }

    private suspend fun stream(
        request: Request,
        sink: OutputStream,
        onProgress: (Long, Long?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        streamClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw error(response.code, response.body.string())
            }
            val body = response.body
            val total = body.contentLength().takeIf { it > 0 }
            body.byteStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var delivered = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    sink.write(buffer, 0, read)
                    delivered += read
                    onProgress(delivered, total)
                }
            }
            sink.flush()
        }
    }

    private fun parseEvent(raw: String): TransportEvent? {
        val obj = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull() ?: return null
        fun str(key: String): String? = obj[key]?.jsonPrimitive?.contentOrNull
        val id = str("id") ?: return null

        return when (str("type")) {
            "download-started" -> TransportEvent.Started(id)
            "download-finished" -> TransportEvent.Finished(
                id,
                (obj["files"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
            )
            "download-progress" -> TransportEvent.Progress(id, obj["value"]?.jsonPrimitive?.double ?: 0.0)
            "download-status" -> TransportEvent.Status(id, str("value").orEmpty())
            "download-error" -> TransportEvent.Error(id, str("value").orEmpty())
            "download-state" -> TransportEvent.State(id, str("value").orEmpty())
            "notification" -> TransportEvent.Notification(str("level") ?: "info", str("message").orEmpty())
            else -> null
        }
    }

    companion object {
        private val JSON = "application/json".toMediaType()

        fun create(input: String, token: String): ApiClient? {
            val raw = input.trim()
            if (raw.isEmpty()) return null
            val withScheme =
                if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw
                else "http://$raw"
            val url = withScheme.toHttpUrlOrNull() ?: return null
            return ApiClient(url, token.trim())
        }
    }
}
