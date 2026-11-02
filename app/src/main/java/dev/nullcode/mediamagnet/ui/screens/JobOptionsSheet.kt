package dev.nullcode.mediamagnet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nullcode.mediamagnet.data.ServerSettings
import dev.nullcode.mediamagnet.ui.SharedLink
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

internal val backends = listOf(
    null to "Auto",
    "yt-dlp" to "yt-dlp",
    "gallery-dl" to "gallery-dl",
    "spotdl" to "spotdl",
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
internal fun JobOptionsSheet(
    draft: ServerSettings,
    updateDraft: (ServerSettings) -> Unit,
    backend: String?,
    onBackendChange: (String?) -> Unit,
    cookieDomain: String,
    onCookieDomainChange: (String) -> Unit,
    cookieContent: String,
    onCookieContentChange: (String) -> Unit,
    canStart: Boolean,
    header: @Composable ColumnScope.() -> Unit,
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
            header()

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ShareOptionsSheet(
    shared: SharedLink,
    initialSettings: ServerSettings,
    onDismiss: () -> Unit,
    onStart: (String, String?, String, String, String) -> Unit,
) {
    var draft by remember(shared) { mutableStateOf(initialSettings) }
    var backend by remember(shared) { mutableStateOf<String?>(null) }
    var cookieDomain by remember(shared) { mutableStateOf("") }
    var cookieContent by remember(shared) { mutableStateOf("") }

    JobOptionsSheet(
        draft = draft,
        updateDraft = { draft = it },
        backend = backend,
        onBackendChange = { backend = it },
        cookieDomain = cookieDomain,
        onCookieDomainChange = { cookieDomain = it },
        cookieContent = cookieContent,
        onCookieContentChange = { cookieContent = it },
        canStart = true,
        header = { SharedLinkHeader(shared.url) },
        onDismiss = onDismiss,
        onStart = {
            onStart(shared.url, backend, jobOverrides(draft, backend), cookieDomain, cookieContent)
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SharedLinkHeader(url: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Text(
                text = "Shared link",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Pick a backend and adjust this download only",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SheetGroup(
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
internal fun ChipRow(
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

internal fun jobOverrides(draft: ServerSettings, backend: String?): String {
    val element = jobJson.encodeToJsonElement(draft).jsonObject
    val keys = buildList {
        addAll(commonKeys)
        if (backend == null || backend == "yt-dlp") addAll(ytDlpKeys)
        if (backend == null || backend == "gallery-dl") addAll(galleryDlKeys)
        if (backend == null || backend == "spotdl") addAll(spotdlKeys)
    }
    return JsonObject(element.filterKeys { it in keys }).toString()
}
