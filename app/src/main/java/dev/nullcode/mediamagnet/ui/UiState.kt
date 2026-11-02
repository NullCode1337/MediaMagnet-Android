package dev.nullcode.mediamagnet.ui

sealed interface Connection {
    data object Idle : Connection
    data object Connecting : Connection
    data class Online(val version: String) : Connection
    data class Offline(val message: String) : Connection
}

data class JobUi(
    val id: String,
    val url: String,
    val state: String,
    val files: List<String>,
    val createdAtMs: Long,
    val progress: Float? = null,
    val status: String? = null,
    val error: String? = null,
) {
    val isRunning: Boolean get() = state == "running"
    val isPaused: Boolean get() = state == "paused"
    val isFinished: Boolean get() = state == "finished"
}

data class TransferUi(
    val key: String,
    val label: String,
    val received: Long,
    val total: Long?,
    val fraction: Float?,
)

data class ServerInfo(
    val root: String? = null,
    val usedPercent: Double? = null,
)

data class UiMessage(val text: String, val isError: Boolean)

data class SharedLink(val url: String, val id: Long = System.nanoTime())
