package dev.nullcode.mediamagnet.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mediamagnet")

@Serializable
data class DownloadRecord(
    val id: String,
    val jobId: String = "",
    val url: String = "",
    val name: String,
    val path: String,
    val uri: String,
    val savedAtMs: Long,
)

data class AppPrefs(
    val baseUrl: String = "",
    val token: String = "",
    val treeUri: String? = null,
    val history: List<DownloadRecord> = emptyList(),
    val settings: ServerSettings = ServerSettings(),
)

class AppPreferences(context: Context) {
    private val store = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    val flow: Flow<AppPrefs> = store.data
        .map(::snapshot)
        .distinctUntilChanged()

    val connectionFlow: Flow<Pair<String, String>> = store.data
        .map { it[BASE_URL].orEmpty() to it[TOKEN].orEmpty() }
        .distinctUntilChanged()

    private fun snapshot(prefs: Preferences) = AppPrefs(
        baseUrl = prefs[BASE_URL].orEmpty(),
        token = prefs[TOKEN].orEmpty(),
        treeUri = prefs[TREE_URI],
        history = decodeHistory(prefs[HISTORY]),
        settings = decodeSettings(prefs[SETTINGS]),
    )

    suspend fun setBaseUrl(value: String) {
        store.edit { it[BASE_URL] = value.trim() }
    }

    suspend fun setToken(value: String) {
        store.edit { it[TOKEN] = value.trim() }
    }

    suspend fun setTreeUri(value: String?) {
        store.edit { prefs ->
            if (value == null) prefs.remove(TREE_URI) else prefs[TREE_URI] = value
        }
    }

    suspend fun addHistory(record: DownloadRecord) {
        store.edit { prefs ->
            prefs[HISTORY] = json.encodeToString(listOf(record) + decodeHistory(prefs[HISTORY]))
        }
    }

    suspend fun removeHistory(jobId: String) {
        store.edit { prefs ->
            prefs[HISTORY] = json.encodeToString(
                decodeHistory(prefs[HISTORY]).filterNot { it.jobId.ifBlank { it.id } == jobId },
            )
        }
    }

    suspend fun setSettings(settings: ServerSettings) {
        store.edit { it[SETTINGS] = json.encodeToString(settings) }
    }

    suspend fun addPendingUri(uri: String) {
        store.edit { prefs ->
            val uris = decodeUris(prefs[PENDING_URIS])
            if (uri !in uris) prefs[PENDING_URIS] = json.encodeToString(uris + uri)
        }
    }

    suspend fun removePendingUri(uri: String) {
        store.edit { prefs ->
            prefs[PENDING_URIS] = json.encodeToString(decodeUris(prefs[PENDING_URIS]) - uri)
        }
    }

    suspend fun takePendingUris(): List<String> {
        val current = decodeUris(store.data.first()[PENDING_URIS])
        if (current.isNotEmpty()) store.edit { it[PENDING_URIS] = "[]" }
        return current
    }

    private fun decodeHistory(value: String?): List<DownloadRecord> =
        value?.let { runCatching { json.decodeFromString<List<DownloadRecord>>(it) }.getOrNull() }
            .orEmpty()

    private fun decodeUris(value: String?): List<String> =
        value?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            .orEmpty()

    private fun decodeSettings(value: String?): ServerSettings =
        value?.let { runCatching { json.decodeFromString<ServerSettings>(it) }.getOrNull() }
            ?: ServerSettings()

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val TOKEN = stringPreferencesKey("token")
        val TREE_URI = stringPreferencesKey("tree_uri")
        val HISTORY = stringPreferencesKey("download_history")
        val PENDING_URIS = stringPreferencesKey("pending_uris")
        val SETTINGS = stringPreferencesKey("local_settings")
    }
}
