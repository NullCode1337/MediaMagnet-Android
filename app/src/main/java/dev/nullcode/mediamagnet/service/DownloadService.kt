package dev.nullcode.mediamagnet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.nullcode.mediamagnet.MainActivity
import dev.nullcode.mediamagnet.R
import dev.nullcode.mediamagnet.ui.JobUi
import dev.nullcode.mediamagnet.ui.MediaMagnetEngine
import dev.nullcode.mediamagnet.ui.TransferUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastUpdate = 0L
    private var lastActive: Boolean? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val engine = MediaMagnetEngine.get(this)
        start(notification(engine.jobs.value.values.toList(), engine.transfers.value.values.toList()))

        scope.launch {
            combine(engine.jobs, engine.transfers) { jobs, transfers ->
                jobs.values.toList() to transfers.values.toList()
            }.collect { (jobs, transfers) ->
                val now = SystemClock.elapsedRealtime()
                val active = jobs.any { it.isRunning || it.isPaused } || transfers.isNotEmpty()
                if (active == lastActive && now - lastUpdate < THROTTLE_MS) return@collect
                lastUpdate = now
                lastActive = active
                NotificationManagerCompat.from(this@DownloadService)
                    .notify(NOTIFICATION_ID, notification(jobs, transfers))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun start(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows progress while MediaMagnet downloads and saves files"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(jobs: List<JobUi>, transfers: List<TransferUi>): Notification {
        val running = jobs.count { it.isRunning }
        val paused = jobs.count { it.isPaused }
        val active = running + paused + transfers.size
        val title = when {
            active == 0 -> "MediaMagnet"
            running == 0 && transfers.isEmpty() -> "Downloads paused"
            else -> "Downloading"
        }
        val text = buildList {
            if (running > 0) add("$running running")
            if (paused > 0) add("$paused paused")
            if (transfers.isNotEmpty()) {
                add("saving ${transfers.size} file${if (transfers.size == 1) "" else "s"}")
            }
        }.joinToString(" · ").ifBlank { "Waiting for downloads" }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val progress = jobs.singleOrNull()?.takeIf { it.isRunning }?.progress
        if (progress != null) {
            builder.setProgress(100, (progress * 100).toInt(), false)
        } else if (active > 0) {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val CHANNEL_ID = "downloads"
        const val NOTIFICATION_ID = 1
        const val THROTTLE_MS = 750L
    }
}
