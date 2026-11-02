package dev.nullcode.mediamagnet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nullcode.mediamagnet.ui.AppViewModel
import dev.nullcode.mediamagnet.ui.MediaMagnetApp
import dev.nullcode.mediamagnet.ui.SharedLink
import dev.nullcode.mediamagnet.ui.theme.MediaMagnetTheme

private const val KEY_SHARED_URL = "shared_url"

class MainActivity : ComponentActivity() {
    private val storagePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private var sharedLink by mutableStateOf<SharedLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedLink = savedInstanceState?.getString(KEY_SHARED_URL)?.let(::sharedLinkOf)
            ?: intent.sharedUrl()?.let(::sharedLinkOf)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            MediaMagnetTheme(
                themeMode = settings.themeMode,
                amoled = settings.amoledMode,
                accentMode = settings.accentMode,
                accentHue = settings.accentHue.toFloat(),
            ) {
                MediaMagnetApp(
                    viewModel = viewModel,
                    sharedLink = sharedLink,
                    onSharedLinkConsumed = { sharedLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.sharedUrl()?.let { sharedLink = sharedLinkOf(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        sharedLink?.let { outState.putString(KEY_SHARED_URL, it.url) }
    }
}

private fun Intent.sharedUrl(): String? = when (action) {
    Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
    Intent.ACTION_VIEW -> dataString
    else -> null
}?.let(::firstUrlIn)?.takeIf { it.isNotEmpty() }

private fun sharedLinkOf(url: String) = SharedLink(url)

private val urlPattern = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

private fun firstUrlIn(text: String): String {
    val match = urlPattern.find(text)?.value ?: text
    return match.trim().trimEnd('.', ',', ')', ']', '"', '\'')
}
