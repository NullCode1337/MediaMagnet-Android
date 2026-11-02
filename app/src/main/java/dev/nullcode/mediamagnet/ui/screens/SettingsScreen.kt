package dev.nullcode.mediamagnet.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dev.nullcode.mediamagnet.data.AccentMode
import dev.nullcode.mediamagnet.data.ServerSettings
import dev.nullcode.mediamagnet.data.SiteArgument
import dev.nullcode.mediamagnet.data.ThemeMode
import dev.nullcode.mediamagnet.ui.AppViewModel
import dev.nullcode.mediamagnet.ui.Connection
import dev.nullcode.mediamagnet.ui.components.SectionCard
import dev.nullcode.mediamagnet.ui.theme.AppPalettes
import dev.nullcode.mediamagnet.ui.theme.supportsDynamicColor

internal val recodeFormats = listOf("", "mp4", "mkv", "webm", "mp3", "m4a", "opus", "flac", "wav")

@Composable
fun SettingsScreen(onOpen: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            SettingsGroup(
                title = "Configuration",
                entries = listOf(
                    SettingsEntryData(Icons.Rounded.Dns, "Server connection", "settings/connection"),
                    SettingsEntryData(Icons.Rounded.Palette, "Appearance", "settings/appearance"),
                    SettingsEntryData(Icons.Rounded.Download, "Downloads", "settings/downloads"),
                    SettingsEntryData(Icons.Rounded.Cookie, "Cookies", "settings/cookies"),
                ),
                onOpen = onOpen,
            )
        }

        item {
            SettingsGroup(
                title = "Backend",
                entries = listOf(
                    SettingsEntryData(Icons.Rounded.Movie, "yt-dlp", "settings/ytdlp"),
                    SettingsEntryData(Icons.Rounded.Image, "gallery-dl", "settings/gallerydl"),
                    SettingsEntryData(Icons.Rounded.MusicNote, "spotdl", "settings/spotdl"),
                    SettingsEntryData(Icons.Rounded.Handyman, "Tools", "settings/tools"),
                ),
                onOpen = onOpen,
            )
        }

        item {
            SettingsGroup(
                title = "About",
                entries = listOf(
                    SettingsEntryData(Icons.Rounded.Info, "About", "settings/about"),
                ),
                onOpen = onOpen,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsGroup(
    title: String,
    entries: List<SettingsEntryData>,
    onOpen: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            entries.forEachIndexed { index, entry ->
                ListItem(
                    onClick = { onOpen(entry.route) },
                    leadingContent = { SettingsIconBadge(entry.icon) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.segmentedColors(),
                    shapes = ListItemDefaults.segmentedShapes(
                        index = index,
                        count = entries.size,
                    ),
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsIconBadge(icon: ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

private data class SettingsEntryData(
    val icon: ImageVector,
    val label: String,
    val route: String,
)

@Composable
fun ConnectionSettingsScreen(viewModel: AppViewModel) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()

    var baseUrl by rememberSaveable { mutableStateOf(prefs.baseUrl) }
    var token by rememberSaveable { mutableStateOf(prefs.token) }
    LaunchedEffect(prefs.baseUrl) { baseUrl = prefs.baseUrl }
    LaunchedEffect(prefs.token) { token = prefs.token }

    val status = when (val state = connection) {
        is Connection.Online -> "Connected to MediaMagnet ${state.version}"
        is Connection.Offline -> "Offline: ${state.message}"
        Connection.Connecting -> "Connecting…"
        Connection.Idle -> "Not configured"
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsCard(title = "Server", subtitle = status) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.10:8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            SettingsCard(
                title = "Authentication",
                subtitle = "Optional. Required only if the server sets a token.",
            ) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Bearer token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        viewModel.setBaseUrl(baseUrl)
                        viewModel.setToken(token)
                    },
                    enabled = baseUrl.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Text("Save", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = viewModel::refresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("Test", modifier = Modifier.padding(start = 8.dp))
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
        title = "Downloads",
        subtitle = "Stored on this phone and sent with every job.",
    ) { draft, update ->
        SettingsCard(
            title = "Local storage",
            subtitle = "Where finished downloads are saved on this phone",
        ) {
            Text(
                text = preferredFolderLabel(prefs.treeUri),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Downloads/MediaMagnet keeps a folder in the shared Downloads collection. " +
                    "A picked folder uses Android's Storage Access Framework and its grant is " +
                    "persisted across reboots.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { folderPicker.launch(null) }) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                    Text("Choose folder", modifier = Modifier.padding(start = 8.dp))
                }
                if (prefs.treeUri != null) {
                    OutlinedButton(onClick = { viewModel.setTreeUri(null) }) {
                        Text("Use Downloads")
                    }
                }
            }
        }

        SettingsCard(
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
                LinearWavyProgressIndicator(
                    progress = { (used / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = "Disk usage is unavailable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsCard(title = "Behaviour") {
            SwitchRow("Preserve source modification time", draft.preserveMtime) {
                update(draft.copy(preserveMtime = it))
            }
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
        SettingsCard(
            title = "Theme",
            subtitle = if (draft.themeMode == ThemeMode.System) {
                "Following the system light/dark setting"
            } else {
                "Overriding the system light/dark setting"
            },
        ) {
            OptionRow(
                options = listOf(
                    ThemeMode.System to "System",
                    ThemeMode.Light to "Light",
                    ThemeMode.Dark to "Dark",
                ),
                selected = draft.themeMode,
                onSelect = { update(draft.copy(themeMode = it)) },
            )
            SwitchRow("AMOLED black", draft.amoledMode) {
                update(draft.copy(amoledMode = it))
            }
        }

        SettingsCard(
            title = "Accent color",
            subtitle = if (draft.accentMode == AccentMode.System) {
                if (supportsDynamicColor) {
                    "Using the wallpaper palette Android derives for this device"
                } else {
                    "Wallpaper colors need Android 12 or newer; using the default accent"
                }
            } else {
                "Using a custom hue"
            },
        ) {
            OptionRow(
                options = listOf(
                    AccentMode.System to "System",
                    AccentMode.Custom to "Custom",
                ),
                selected = draft.accentMode,
                onSelect = { update(draft.copy(accentMode = it)) },
            )
            if (draft.accentMode == AccentMode.Custom) {
                HueSlider(
                    hue = draft.accentHue.toFloat(),
                    onChange = { update(draft.copy(accentHue = it.toInt())) },
                )
            }
        }

        SettingsCard(
            title = "Network",
            subtitle = "Sent with every request to the server",
        ) {
            OutlinedTextField(
                value = draft.userAgent,
                onValueChange = { update(draft.copy(userAgent = it)) },
                label = { Text("User agent") },
                placeholder = { Text("Default") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionCard(title = title, subtitle = subtitle, trailing = trailing) {
        content()
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YtDlpSettingsScreen(viewModel: AppViewModel) {
    ServerSettingsPage(viewModel = viewModel, title = "yt-dlp") { draft, update ->
        SettingsCard(
            title = "Format",
            subtitle = "What yt-dlp selects and what it converts to",
        ) {
            OutlinedTextField(
                value = draft.ytFormat,
                onValueChange = { update(draft.copy(ytFormat = it)) },
                label = { Text("Format selector") },
                placeholder = { Text("bv*+ba/b") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FieldLabel("Recode to")
            FormatChips(
                selected = draft.ytRecodeFormat,
                onSelect = { update(draft.copy(ytRecodeFormat = it)) },
            )
            OutlinedTextField(
                value = draft.ytOutputTemplate,
                onValueChange = { update(draft.copy(ytOutputTemplate = it)) },
                label = { Text("Output template") },
                placeholder = { Text("%(title)s.%(ext)s") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SettingsCard(title = "Post-processing") {
            SwitchRow("Embed thumbnail", draft.ytEmbedThumbnail) {
                update(draft.copy(ytEmbedThumbnail = it))
            }
            SwitchRow("Embed subtitles", draft.ytEmbedSubs) {
                update(draft.copy(ytEmbedSubs = it))
            }
            SwitchRow("Restrict filenames", draft.ytRestrictFilenames) {
                update(draft.copy(ytRestrictFilenames = it))
            }
        }

        SettingsCard(
            title = "Arguments",
            subtitle = "Passed to yt-dlp verbatim",
        ) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatChips(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recodeFormats.forEach { format ->
            FilterChip(
                selected = selected == format,
                onClick = { onSelect(format) },
                label = { Text(format.ifBlank { "none" }) },
            )
        }
    }
}

@Composable
fun GalleryDlSettingsScreen(viewModel: AppViewModel) {
    ServerSettingsPage(viewModel = viewModel, title = "gallery-dl") { draft, update ->
        SettingsCard(
            title = "Arguments",
            subtitle = "Passed to gallery-dl verbatim",
        ) {
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
}

@Composable
fun SpotdlSettingsScreen(viewModel: AppViewModel) {
    ServerSettingsPage(viewModel = viewModel, title = "spotdl") { draft, update ->
        SettingsCard(
            title = "Audio",
            subtitle = "Output format and bitrate",
        ) {
            OutlinedTextField(
                value = draft.spotdlFormat,
                onValueChange = { update(draft.copy(spotdlFormat = it)) },
                label = { Text("Audio format") },
                placeholder = { Text("mp3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.spotdlBitrate,
                onValueChange = { update(draft.copy(spotdlBitrate = it)) },
                label = { Text("Bitrate") },
                placeholder = { Text("320k") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SettingsCard(
            title = "Arguments",
            subtitle = "Passed to spotdl verbatim",
        ) {
            OutlinedTextField(
                value = draft.spotdlGlobalArgs,
                onValueChange = { update(draft.copy(spotdlGlobalArgs = it)) },
                label = { Text("Global arguments") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsCard(
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
            SettingsCard(title = "Server") {
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
    content: @Composable ColumnScope.(ServerSettings, (ServerSettings) -> Unit) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(stateSaver = SettingsSaver) { mutableStateOf(settings) }
    LaunchedEffect(settings) { draft = settings }
    val dirty = draft != settings

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content(draft) { draft = it }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { viewModel.saveSettings(draft) },
                    enabled = dirty,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Text("Save", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = { confirmReset = true }) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                    Text("Reset", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        item {
            Text(
                text = if (dirty) {
                    "Unsaved changes · settings version ${draft.settingsVersion}"
                } else {
                    "All changes saved · settings version ${draft.settingsVersion}"
                },
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

private val draftJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private val SettingsSaver = Saver<ServerSettings, String>(
    save = { draftJson.encodeToString(it) },
    restore = { runCatching { draftJson.decodeFromString<ServerSettings>(it) }.getOrNull() },
)

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

@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    val primary = remember(hue) { AppPalettes(hue, monochrome = false).primary[40] }
    val spectrum = remember {
        listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { stop ->
            AppPalettes(stop, monochrome = false).primary[60]
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Slider(
            value = hue,
            onValueChange = onChange,
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = primary,
                activeTrackColor = primary,
            ),
            modifier = Modifier.drawBehind {
                val trackHeight = 16.dp.toPx()
                val top = (size.height - trackHeight) / 2f
                drawRoundRect(
                    brush = Brush.horizontalGradient(spectrum),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f),
                    alpha = 0.35f,
                )
            },
        )
        Text(
            text = "Hue ${hue.toInt()}°",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OptionRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) },
            )
        }
    }
}
