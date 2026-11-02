package dev.nullcode.mediamagnet.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nullcode.mediamagnet.data.ServerSettings
import dev.nullcode.mediamagnet.ui.AppViewModel
import dev.nullcode.mediamagnet.ui.JobUi
import dev.nullcode.mediamagnet.ui.components.StatePill
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.text.DateFormat
import java.util.Date

private val backends = listOf(
    null to "Auto",
    "yt-dlp" to "yt-dlp",
    "gallery-dl" to "gallery-dl",
    "spotdl" to "spotdl",
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun DownloadsScreen(
    viewModel: AppViewModel,
    onOpenSettings: () -> Unit,
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val localSettings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var url by rememberSaveable { mutableStateOf("") }
    var backend by rememberSaveable { mutableStateOf<String?>(null) }
    var cookieDomain by rememberSaveable { mutableStateOf("") }
    var cookieContent by rememberSaveable { mutableStateOf("") }
    var draft by remember { mutableStateOf(localSettings) }
    var showOptions by rememberSaveable { mutableStateOf(false) }

    val sorted = remember(jobs) {
        val ordered = jobs.values.sortedByDescending { it.createdAtMs }
        ordered.filter { it.isRunning || it.isPaused } to
            ordered.filterNot { it.isRunning || it.isPaused }
    }
    val (active, recent) = sorted

    fun start() {
        viewModel.startDownload(
            url,
            backend,
            jobOverrides(draft, backend),
            cookieDomain,
            cookieContent,
        )
        url = ""
        showOptions = false
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroHeader(
                activeCount = active.size,
                configured = prefs.baseUrl.isNotBlank(),
            )
        }

        if (prefs.baseUrl.isBlank()) {
            item { ConnectBanner(onOpenSettings) }
        }

        item {
            ComposerCard(
                url = url,
                onUrlChange = { url = it },
                backend = backend,
                onBackendChange = { backend = it },
                onPaste = {
                    readClipboard(context)?.let { url = it }
                },
                onOptions = { showOptions = true },
                onStart = { start() },
            )
        }

        if (active.isEmpty() && recent.isEmpty()) {
            item { EmptyState() }
        } else {
            if (active.isNotEmpty()) {
                item {
                    SectionHeader(title = "In progress", count = active.size) {
                        TextButton(onClick = viewModel::cancelAll) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("Cancel all", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                items(active, key = { it.id }) { job ->
                    JobCard(
                        job = job,
                        onPause = { viewModel.pause(job.id) },
                        onResume = { viewModel.resume(job.id) },
                        onCancel = { viewModel.cancel(job.id) },
                    )
                }
            }

            if (recent.isNotEmpty()) {
                item { SectionHeader(title = "Recent", count = recent.size) }
                items(recent, key = { it.id }) { job ->
                    JobCard(
                        job = job,
                        onPause = { viewModel.pause(job.id) },
                        onResume = { viewModel.resume(job.id) },
                        onCancel = { viewModel.cancel(job.id) },
                    )
                }
            }
        }
    }

    if (showOptions) {
        OptionsSheet(
            draft = draft,
            updateDraft = { draft = it },
            backend = backend,
            onBackendChange = { backend = it },
            cookieDomain = cookieDomain,
            onCookieDomainChange = { cookieDomain = it },
            cookieContent = cookieContent,
            onCookieContentChange = { cookieContent = it },
            canStart = url.isNotBlank(),
            onDismiss = { showOptions = false },
            onStart = { start() },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroHeader(activeCount: Int, configured: Boolean) {
    Column(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "MediaMagnet",
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (activeCount > 0) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(22.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = activeCount.toString(),
                            style = MaterialTheme.typography.labelSmallEmphasized,
                        )
                    }
                }
            }
        }
        Text(
            text = when {
                !configured -> "Not connected to a server"
                activeCount == 1 -> "1 download in progress"
                activeCount > 1 -> "$activeCount downloads in progress"
                else -> "Ready when you are"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectBanner(onOpenSettings: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Connect your server",
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Text(
                text = "MediaMagnet needs the address of your server before it can download anything.",
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = onOpenSettings) {
                Text("Open settings")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ComposerCard(
    url: String,
    onUrlChange: (String) -> Unit,
    backend: String?,
    onBackendChange: (String?) -> Unit,
    onPaste: () -> Unit,
    onOptions: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "New download",
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (url.isNotBlank()) {
                LinkPreviewCard(url)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Link",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    placeholder = { Text("Paste a video, gallery or track URL") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    trailingIcon = {
                        FilledTonalIconButton(onClick = onPaste) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste link")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                backends.forEach { (value, label) ->
                    FilterChip(
                        selected = backend == value,
                        onClick = { onBackendChange(value) },
                        label = { Text(label) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onOptions) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Options", modifier = Modifier.padding(start = 8.dp))
                }
                Button(
                    onClick = onStart,
                    enabled = url.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.LargeContentPadding,
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkPreviewCard(url: String) {
    val preview = remember(url) { previewFor(url) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = preview.title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = preview.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(preview.icon, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (count > 0) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "No downloads yet",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Paste a link above to start one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun JobCard(
    job: JobUi,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val preview = remember(job.url) { previewFor(job.url) }
    val created = remember(job.createdAtMs) {
        if (job.createdAtMs > 0) formatTime(job.createdAtMs) else null
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(preview.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = preview.subtitle,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (created != null) {
                        Text(
                            text = created,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatePill(job.state)
            }

            Text(
                text = job.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (job.isRunning || job.isPaused) {
                val progress = job.progress
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (progress != null) {
                        LinearWavyProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (progress != null) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (job.status != null) {
                Text(
                    text = job.status,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (job.error != null) {
                Text(
                    text = job.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (job.files.isNotEmpty()) {
                Text(
                    text = "${job.files.size} file${if (job.files.size == 1) "" else "s"} saved automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (job.isRunning || job.isPaused) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (job.isPaused) {
                        FilledTonalButton(onClick = onResume) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("Resume", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    if (job.isRunning) {
                        FilledTonalButton(onClick = onPause) {
                            Icon(
                                Icons.Rounded.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("Pause", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
private fun OptionsSheet(
    draft: ServerSettings,
    updateDraft: (ServerSettings) -> Unit,
    backend: String?,
    onBackendChange: (String?) -> Unit,
    cookieDomain: String,
    onCookieDomainChange: (String) -> Unit,
    cookieContent: String,
    onCookieContentChange: (String) -> Unit,
    canStart: Boolean,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
                Text(
                    text = "Configure before downloading",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Adjust this download only",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            SheetGroup("General") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    backends.forEach { (value, label) ->
                        FilterChip(
                            selected = backend == value,
                            onClick = { onBackendChange(value) },
                            label = { Text(label) },
                        )
                    }
                }
                SwitchRow("Preserve source modification time", draft.preserveMtime) {
                    updateDraft(draft.copy(preserveMtime = it))
                }
                OutlinedTextField(
                    value = draft.userAgent,
                    onValueChange = { updateDraft(draft.copy(userAgent = it)) },
                    label = { Text("User agent") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (backend == null || backend == "yt-dlp") {
                SheetGroup("yt-dlp") {
                    OutlinedTextField(
                        value = draft.ytFormat,
                        onValueChange = { updateDraft(draft.copy(ytFormat = it)) },
                        label = { Text("Format selector") },
                        placeholder = { Text("bv*+ba/b") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Recode to",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChipRow(
                        options = recodeFormats.map { it to it.ifBlank { "none" } },
                        selected = draft.ytRecodeFormat,
                        onSelect = { updateDraft(draft.copy(ytRecodeFormat = it)) },
                    )
                    OutlinedTextField(
                        value = draft.ytOutputTemplate,
                        onValueChange = { updateDraft(draft.copy(ytOutputTemplate = it)) },
                        label = { Text("Output template") },
                        placeholder = { Text("%(title)s.%(ext)s") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchRow("Embed thumbnail", draft.ytEmbedThumbnail) {
                        updateDraft(draft.copy(ytEmbedThumbnail = it))
                    }
                    SwitchRow("Embed subtitles", draft.ytEmbedSubs) {
                        updateDraft(draft.copy(ytEmbedSubs = it))
                    }
                    SwitchRow("Restrict filenames", draft.ytRestrictFilenames) {
                        updateDraft(draft.copy(ytRestrictFilenames = it))
                    }
                    OutlinedTextField(
                        value = draft.ytGlobalArgs,
                        onValueChange = { updateDraft(draft.copy(ytGlobalArgs = it)) },
                        label = { Text("Global arguments") },
                        minLines = 2,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SiteArgsEditor(
                        label = "yt-dlp",
                        args = draft.ytSiteArgs,
                        onChange = { updateDraft(draft.copy(ytSiteArgs = it)) },
                    )
                }
            }

            if (backend == null || backend == "gallery-dl") {
                SheetGroup("gallery-dl") {
                    OutlinedTextField(
                        value = draft.gdlGlobalArgs,
                        onValueChange = { updateDraft(draft.copy(gdlGlobalArgs = it)) },
                        label = { Text("Global arguments") },
                        minLines = 2,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SiteArgsEditor(
                        label = "gallery-dl",
                        args = draft.gdlSiteArgs,
                        onChange = { updateDraft(draft.copy(gdlSiteArgs = it)) },
                    )
                }
            }

            if (backend == null || backend == "spotdl") {
                SheetGroup("spotdl") {
                    OutlinedTextField(
                        value = draft.spotdlFormat,
                        onValueChange = { updateDraft(draft.copy(spotdlFormat = it)) },
                        label = { Text("Audio format") },
                        placeholder = { Text("mp3") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.spotdlBitrate,
                        onValueChange = { updateDraft(draft.copy(spotdlBitrate = it)) },
                        label = { Text("Bitrate") },
                        placeholder = { Text("320k") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.spotdlGlobalArgs,
                        onValueChange = { updateDraft(draft.copy(spotdlGlobalArgs = it)) },
                        label = { Text("Global arguments") },
                        minLines = 2,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SheetGroup("Cookies") {
                OutlinedTextField(
                    value = cookieDomain,
                    onValueChange = onCookieDomainChange,
                    label = { Text("Cookie domain (optional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cookieContent,
                    onValueChange = onCookieContentChange,
                    label = { Text("Cookie content (Netscape or JSON)") },
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.LargeContentPadding,
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SheetGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

private data class LinkPreview(val title: String, val subtitle: String, val icon: ImageVector)

private fun previewFor(raw: String): LinkPreview {
    val uri = runCatching { Uri.parse(raw) }.getOrNull()
    val host = uri?.host?.removePrefix("www.").orEmpty().ifBlank { raw.take(40) }
    val icon = when {
        host.contains("youtube") || host.contains("youtu.be") ||
            host.contains("vimeo") || host.contains("twitch") -> Icons.Rounded.Movie
        host.contains("spotify") || host.contains("soundcloud") ||
            host.contains("bandcamp") -> Icons.Rounded.MusicNote
        host.contains("instagram") || host.contains("pinterest") ||
            host.contains("flickr") || host.contains("x.com") ||
            host.contains("twitter") -> Icons.Rounded.Image
        else -> Icons.Rounded.Link
    }
    val segment = uri?.pathSegments?.lastOrNull()?.takeIf { it.isNotBlank() }
    val title = segment?.let { Uri.decode(it) } ?: host
    return LinkPreview(title = title, subtitle = host, icon = icon)
}

private fun readClipboard(context: Context): String? {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}

private val timeFormat: DateFormat =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

private fun formatTime(ms: Long): String = synchronized(timeFormat) { timeFormat.format(Date(ms)) }

private val jobJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private val commonKeys = listOf("user_agent", "preserve_mtime")
private val ytDlpKeys = listOf(
    "yt_format",
    "yt_recode_format",
    "yt_output_template",
    "yt_embed_thumbnail",
    "yt_embed_subs",
    "yt_restrict_filenames",
    "yt_global_args",
    "yt_site_args",
)
private val galleryDlKeys = listOf("gdl_global_args", "gdl_site_args")
private val spotdlKeys = listOf("spotdl_format", "spotdl_bitrate", "spotdl_global_args")

private fun jobOverrides(draft: ServerSettings, backend: String?): String {
    val element = jobJson.encodeToJsonElement(draft).jsonObject
    val keys = buildList {
        addAll(commonKeys)
        if (backend == null || backend == "yt-dlp") addAll(ytDlpKeys)
        if (backend == null || backend == "gallery-dl") addAll(galleryDlKeys)
        if (backend == null || backend == "spotdl") addAll(spotdlKeys)
    }
    return JsonObject(element.filterKeys { it in keys }).toString()
}
