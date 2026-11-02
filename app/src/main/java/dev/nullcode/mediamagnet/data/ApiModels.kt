package dev.nullcode.mediamagnet.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Health(val status: String, val version: String)

@Serializable
data class ApiErrorBody(val error: String, val message: String)

@Serializable
data class MultipleFilesBody(
    val error: String,
    val message: String,
    val files: List<String> = emptyList(),
)

@Serializable
data class DownloadAccepted(val id: String)

@Serializable
data class JobCookie(val domain: String, val content: String)

@Serializable
data class JobSummary(
    val id: String,
    val url: String,
    val state: String,
    val files: List<String> = emptyList(),
    @SerialName("created_at_ms") val createdAtMs: Long = 0,
)

@Serializable
data class JobList(val jobs: List<JobSummary> = emptyList())

@Serializable
data class DownloadRequest(
    val url: String,
    val id: String? = null,
    val settings: ServerSettings? = null,
    val backend: String? = null,
    val overrides: JsonObject? = null,
    val cookies: List<JobCookie>? = null,
)

@Serializable
data class SiteArgument(
    val id: String = "",
    val domain: String = "",
    val args: String = "",
)

object ThemeMode {
    const val System = "system"
    const val Light = "light"
    const val Dark = "dark"
}

object AccentMode {
    const val System = "system"
    const val Custom = "custom"
}

@Serializable
data class ServerSettings(
    @SerialName("settings_version") val settingsVersion: Int = 1,
    @SerialName("theme_mode") val themeMode: String = ThemeMode.System,
    @SerialName("amoled_mode") val amoledMode: Boolean = false,
    @SerialName("accent_mode") val accentMode: String = AccentMode.System,
    @SerialName("accent_hue") val accentHue: Int = 322,
    @SerialName("user_agent") val userAgent: String = "",
    @SerialName("preserve_mtime") val preserveMtime: Boolean = false,
    @SerialName("yt_format") val ytFormat: String = "bv*+ba/b",
    @SerialName("yt_recode_format") val ytRecodeFormat: String = "",
    @SerialName("yt_output_template") val ytOutputTemplate: String = "%(title)s.%(ext)s",
    @SerialName("yt_embed_thumbnail") val ytEmbedThumbnail: Boolean = false,
    @SerialName("yt_embed_subs") val ytEmbedSubs: Boolean = false,
    @SerialName("yt_restrict_filenames") val ytRestrictFilenames: Boolean = false,
    @SerialName("yt_global_args") val ytGlobalArgs: String = "",
    @SerialName("yt_site_args") val ytSiteArgs: List<SiteArgument> = emptyList(),
    @SerialName("gdl_global_args") val gdlGlobalArgs: String = "",
    @SerialName("gdl_site_args") val gdlSiteArgs: List<SiteArgument> = emptyList(),
    @SerialName("spotdl_format") val spotdlFormat: String = "",
    @SerialName("spotdl_bitrate") val spotdlBitrate: String = "",
    @SerialName("spotdl_global_args") val spotdlGlobalArgs: String = "",
)

@Serializable
data class CookieInput(val type: String, val value: String)

@Serializable
data class SaveCookieRequest(val domain: String, val input: CookieInput)

@Serializable
data class SavedCookie(val domain: String, val path: String)

@Serializable
data class DeletedCount(val deleted: Int)

@Serializable
data class DownloadRoot(val path: String)

@Serializable
data class DiskUsage(@SerialName("used_percent") val usedPercent: Double)

@Serializable
data class ToolStatus(
    val name: String,
    val available: Boolean,
    val source: String? = null,
    val version: String? = null,
    val program: String? = null,
    val args: List<String> = emptyList(),
    val error: String? = null,
)

@Serializable
data class ToolList(
    val tools: List<ToolStatus> = emptyList(),
    val deno: String? = null,
    val ffmpeg: String? = null,
)

@Serializable
data class ToolUpdate(
    val updated: List<String> = emptyList(),
    val output: String = "",
)

sealed interface TransportEvent {
    data class Started(val id: String) : TransportEvent
    data class Finished(val id: String, val files: List<String>) : TransportEvent
    data class Progress(val id: String, val value: Double) : TransportEvent
    data class Status(val id: String, val value: String) : TransportEvent
    data class Error(val id: String, val value: String) : TransportEvent
    data class State(val id: String, val value: String) : TransportEvent
    data class Notification(val level: String, val message: String) : TransportEvent
}
