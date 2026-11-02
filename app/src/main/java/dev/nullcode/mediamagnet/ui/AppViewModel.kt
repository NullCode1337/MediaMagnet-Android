package dev.nullcode.mediamagnet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dev.nullcode.mediamagnet.data.AppPrefs
import dev.nullcode.mediamagnet.data.Health
import dev.nullcode.mediamagnet.data.ServerSettings
import dev.nullcode.mediamagnet.data.ToolList
import dev.nullcode.mediamagnet.data.ToolUpdate
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = MediaMagnetEngine.get(app)

    val prefs: StateFlow<AppPrefs> = engine.prefs
    val connection: StateFlow<Connection> = engine.connection
    val health: StateFlow<Health?> = engine.health
    val jobs: StateFlow<Map<String, JobUi>> = engine.jobs
    val settings: StateFlow<ServerSettings> = engine.settings
    val cookies: StateFlow<Map<String, String>> = engine.cookies
    val serverInfo: StateFlow<ServerInfo> = engine.serverInfo
    val tools: StateFlow<ToolList?> = engine.tools
    val toolsUpdating: StateFlow<Boolean> = engine.toolsUpdating
    val toolUpdate: StateFlow<ToolUpdate?> = engine.toolUpdate
    val transfers: StateFlow<Map<String, TransferUi>> = engine.transfers
    val messages: SharedFlow<UiMessage> = engine.messages

    fun startDownload(
        url: String,
        backend: String?,
        overridesJson: String,
        cookieDomain: String,
        cookieContent: String,
    ) = engine.startDownload(url, backend, overridesJson, cookieDomain, cookieContent)

    fun pause(id: String) = engine.pause(id)
    fun resume(id: String) = engine.resume(id)
    fun cancel(id: String) = engine.cancel(id)
    fun cancelAll() = engine.cancelAll()
    fun removeHistory(jobId: String) = engine.removeHistory(jobId)
    fun refresh() = engine.refresh()
    fun saveSettings(settings: ServerSettings) = engine.saveSettings(settings)
    fun resetSettings() = engine.resetSettings()
    fun loadCookies() = engine.loadCookies()
    fun saveCookie(domain: String, type: String, value: String) = engine.saveCookie(domain, type, value)
    fun deleteCookie(domain: String) = engine.deleteCookie(domain)
    fun clearCookies() = engine.clearCookies()
    fun loadTools() = engine.loadTools()
    fun updateTools() = engine.updateTools()
    fun dismissToolUpdate() = engine.dismissToolUpdate()
    fun refreshPaths() = engine.refreshPaths()
    fun setBaseUrl(value: String) = engine.setBaseUrl(value)
    fun setToken(value: String) = engine.setToken(value)
    fun setTreeUri(value: String?) = engine.setTreeUri(value)
    fun emit(text: String, isError: Boolean) = engine.emit(text, isError)
}
