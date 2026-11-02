package dev.nullcode.mediamagnet.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nullcode.mediamagnet.data.ServerSettings
import dev.nullcode.mediamagnet.data.SiteArgument
import dev.nullcode.mediamagnet.ui.AppViewModel
import dev.nullcode.mediamagnet.ui.Connection
import dev.nullcode.mediamagnet.ui.components.SectionCard

internal val recodeFormats = listOf("", "mp4", "mkv", "webm", "mp3", "m4a", "opus", "flac", "wav")

@Composable
fun SettingsScreen(onOpen: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        item {
            SettingsGroup("Configuration") {
                SettingsEntry(Icons.Rounded.Dns, "Server connection") { onOpen("settings/connection") }
                EntryDivider()
                SettingsEntry(Icons.Rounded.Palette, "Appearance") { onOpen("settings/appearance") }
                EntryDivider()
                SettingsEntry(Icons.Rounded.Download, "Downloads") { onOpen("settings/downloads") }
                EntryDivider()
                SettingsEntry(Icons.Rounded.Cookie, "Cookies") { onOpen("settings/cookies") }
            }
        }

        item {
            SettingsGroup("Backend") {
                SettingsEntry(Icons.Rounded.Movie, "yt-dlp") { onOpen("settings/ytdlp") }
                EntryDivider()
                SettingsEntry(Icons.Rounded.Image, "gallery-dl") { onOpen("settings/gallerydl") }
                EntryDivider()
                SettingsEntry(Icons.Rounded.MusicNote, "spotdl") { onOpen("settings/spotdl") }
                EntryDivider()
                SettingsEntry(Icons.Rounded.Handyman, "Tools") { onOpen("settings/tools") }
            }
        }

        item {
            SettingsGroup("About") {
                SettingsEntry(Icons.Rounded.Info, "About") { onOpen("settings/about") }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsEntry(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EntryDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun ConnectionSettingsScreen(viewModel: AppViewModel) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()

    var baseUrl by rememberSaveable(prefs.baseUrl) { mutableStateOf(prefs.baseUrl) }
    var token by rememberSaveable(prefs.token) { mutableStateOf(prefs.token) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                title = "Server connection",
                subtitle = when (val state = connection) {
                    is Connection.Online -> "Connected to MediaMagnet ${state.version}"
                    is Connection.Offline -> "Offline: ${state.message}"
                    Connection.Connecting -> "Connecting…"
                    Connection.Idle -> "Not configured"
                },
            ) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.10:8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Bearer token (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.setBaseUrl(baseUrl)
                            viewModel.setToken(token)
                        },
                        enabled = baseUrl.isNotBlank(),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Text("Save", modifier = Modifier.padding(start = 4.dp))
                    }
                    OutlinedButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Text("Test", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsSettingsScreen(viewModel: AppViewModel) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.setTreeUri(uri.toString())
        }
    }

    ServerSettingsPage(
        viewModel = viewModel,
        title = "Download behaviour",
        subtitle = "Stored on this phone and sent with every job.",
        extraItems = {
            item {
                SectionCard(
                    title = "Local storage",
                    subtitle = "Where finished downloads are saved on this phone",
                ) {
                    Text(
                        text = preferredFolderLabel(prefs.treeUri),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Downloads/MediaMagnet keeps a folder in the shared Downloads collection. A picked folder uses Android's Storage Access Framework and its grant is persisted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { folderPicker.launch(null) }) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                            Text("Choose folder", modifier = Modifier.padding(start = 4.dp))
                        }
                        if (prefs.treeUri != null) {
                            OutlinedButton(onClick = { viewModel.setTreeUri(null) }) {
                                Text("Use Downloads")
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(
                    title = "Server storage",
                    subtitle = serverInfo.root ?: "Open the server to read its download root",
                    trailing = {
                        IconButton(onClick = viewModel::refreshPaths) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh paths")
                        }
                    },
                ) {
                    val used = serverInfo.usedPercent
                    if (used != null) {
                        Text(
                            text = "Disk usage ${"%.1f".format(used)}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            LinearWavyProgressIndicator(
                                progress = { (used / 100.0).toFloat().coerceIn(0f, 1f) },
                            )
                        }
                    } else {
                        Text(
                            text = "Disk usage is unavailable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) { draft, update ->
        SwitchRow("Preserve source modification time", draft.preserveMtime) {
            update(draft.copy(preserveMtime = it))
        }
    }
}

@Composable
fun AppearanceSettingsScreen(viewModel: AppViewModel) {
    ServerSettingsPage(
        viewModel = viewModel,
        title = "Appearance",
        subtitle = "Stored on this phone.",
    ) { draft, update ->
        SwitchRow("Dark mode", draft.darkMode) { update(draft.copy(darkMode = it)) }
        SwitchRow("AMOLED black", draft.amoledMode) { update(draft.copy(amoledMode = it)) }
        OutlinedTextField(
            value = draft.userAgent,
            onValueChange = { update(draft.copy(userAgent = it)) },
            label = { Text("User agent") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Accent hue ${draft.accentHue}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = draft.accentHue.toFloat(),
            onValueChange = { update(draft.copy(accentHue = it.toInt())) },
            valueRange = 0f..360f,
        )
        OptionRow(
            options = listOf("hue" to "Hue", "black" to "Black", "white" to "White"),
            selected = draft.accentKind,
            onSelect = { update(draft.copy(accentKind = it)) },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YtDlpSettingsScreen(viewModel: AppViewModel) {
    ServerSettingsPage(viewModel = viewModel, title = "yt-dlp") { draft, update ->
        OutlinedTextField(
            value = draft.ytFormat,
            onValueChange = { update(draft.copy(ytFormat = it)) },
            label = { Text("Format selector") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Recode to", style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            recodeFormats.forEach { format ->
                ToggleButton(
                    checked = draft.ytRecodeFormat == format,
                    onCheckedChange = { update(draft.copy(ytRecodeFormat = format)) },
                ) {
                    Text(format.ifBlank { "none" })
                }
            }
        }
        OutlinedTextField(
            value = draft.ytOutputTemplate,
            onValueChange = { update(draft.copy(ytOutputTemplate = it)) },
            label = { Text("Output template") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SwitchRow("Embed thumbnail", draft.ytEmbedThumbnail) {
            update(draft.copy(ytEmbedThumbnail = it))
        }
        SwitchRow("Embed subtitles", draft.ytEmbedSubs) {
            update(draft.copy(ytEmbedSubs = it))
        }
        SwitchRow("Restrict filenames", draft.ytRestrictFilenames) {
            update(draft.copy(ytRestrictFilenames = it))
        }
        OutlinedTextField(
            value = draft.ytGlobalArgs,
            onValueChange = { update(draft.copy(ytGlobalArgs = it)) },
            label = { Text("Global arguments") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        SiteArgsEditor(
            label = "yt-dlp",
            args = draft.ytSiteArgs,
            onChange = { update(draft.copy(ytSiteArgs = it)) },
        )
    }
}

@Composable
fun GalleryDlSettingsScreen(viewModel: AppViewModel) {
    ServerSettingsPage(viewModel = viewModel, title = "gallery-dl") { draft, update ->
        OutlinedTextField(
            value = draft.gdlGlobalArgs,
            onValueChange = { update(draft.copy(gdlGlobalArgs = it)) },
            label = { Text("Global arguments") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        SiteArgsEditor(
            label = "gallery-dl",
            args = draft.gdlSiteArgs,
            onChange = { update(draft.copy(gdlSiteArgs = it)) },
        )
    }
}

@Composable
fun SpotdlSettingsScreen(viewModel: AppViewModel) {
    ServerSettingsPage(viewModel = viewModel, title = "spotdl") { draft, update ->
        OutlinedTextField(
            value = draft.spotdlFormat,
            onValueChange = { update(draft.copy(spotdlFormat = it)) },
            label = { Text("Audio format") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.spotdlBitrate,
            onValueChange = { update(draft.copy(spotdlBitrate = it)) },
            label = { Text("Bitrate") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.spotdlGlobalArgs,
            onValueChange = { update(draft.copy(spotdlGlobalArgs = it)) },
            label = { Text("Global arguments") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AboutScreen(viewModel: AppViewModel) {
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                title = "MediaMagnet for Android",
                subtitle = "Version ${versionName.ifBlank { "unknown" }}",
            ) {
                Text(
                    text = "Native client for a self-hosted MediaMagnet server.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            SectionCard(title = "Server") {
                val status = when (val state = connection) {
                    is Connection.Online -> "Connected to MediaMagnet ${state.version}"
                    is Connection.Offline -> "Offline: ${state.message}"
                    Connection.Connecting -> "Connecting…"
                    Connection.Idle -> "Not configured"
                }
                Text(status, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Settings version ${settings.settingsVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ServerSettingsPage(
    viewModel: AppViewModel,
    title: String,
    subtitle: String? = null,
    extraItems: (LazyListScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(ServerSettings, (ServerSettings) -> Unit) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var draft by remember(settings) { mutableStateOf(settings) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        extraItems?.invoke(this)
        item {
            SectionCard(title = title, subtitle = subtitle) {
                content(draft) { draft = it }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.saveSettings(draft) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Text("Save settings", modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(onClick = { confirmReset = true }) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                    Text("Reset")
                }
            }
        }
        item {
            Text(
                text = "Settings version ${draft.settingsVersion}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset settings?") },
            text = { Text("Your locally stored settings are replaced with defaults. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetSettings()
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

private fun preferredFolderLabel(treeUri: String?): String {
    if (treeUri == null) return "Default: internal storage Downloads/MediaMagnet"
    val decoded = Uri.decode(treeUri)
    val path = decoded.substringAfterLast("tree/").replace(':', '/')
    return "Selected: $path"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SiteArgsEditor(
    label: String,
    args: List<SiteArgument>,
    onChange: (List<SiteArgument>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Per-site arguments", style = MaterialTheme.typography.bodySmall)
        args.forEachIndexed { index, argument ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = argument.domain,
                        onValueChange = { value ->
                            onChange(args.toMutableList().also { it[index] = argument.copy(domain = value) })
                        },
                        label = { Text("Domain") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        onChange(args.filterIndexed { i, _ -> i != index })
                    }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remove rule")
                    }
                }
                OutlinedTextField(
                    value = argument.args,
                    onValueChange = { value ->
                        onChange(args.toMutableList().also { it[index] = argument.copy(args = value) })
                    },
                    label = { Text("Arguments") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        TextButton(onClick = { onChange(args + SiteArgument()) }) {
            Text("Add $label site rule")
        }
    }
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OptionRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            ToggleButton(
                checked = selected == value,
                onCheckedChange = { onSelect(value) },
            ) {
                Text(label)
            }
        }
    }
}
