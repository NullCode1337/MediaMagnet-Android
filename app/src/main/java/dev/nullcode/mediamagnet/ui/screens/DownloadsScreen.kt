package dev.nullcode.mediamagnet.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nullcode.mediamagnet.data.ServerSettings
import dev.nullcode.mediamagnet.ui.AppViewModel
import dev.nullcode.mediamagnet.ui.JobUi
import dev.nullcode.mediamagnet.ui.components.SectionCard
import dev.nullcode.mediamagnet.ui.components.StatePill
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DownloadsScreen(
    viewModel: AppViewModel,
    onOpenSettings: () -> Unit,
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val localSettings by viewModel.settings.collectAsStateWithLifecycle()
    var url by rememberSaveable { mutableStateOf("") }
    var backend by rememberSaveable { mutableStateOf<String?>(null) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var cookieDomain by rememberSaveable { mutableStateOf("") }
    var cookieContent by rememberSaveable { mutableStateOf("") }
    var draftJson by rememberSaveable { mutableStateOf<String?>(null) }
    val draft = remember(draftJson, localSettings) {
        draftJson?.let { runCatching { jobJson.decodeFromString<ServerSettings>(it) }.getOrNull() }
            ?: localSettings
    }
    fun updateDraft(next: ServerSettings) {
        draftJson = jobJson.encodeToString(next)
    }

    val sorted = jobs.values.sortedByDescending { it.createdAtMs }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (prefs.baseUrl.isBlank()) {
            item {
                SectionCard(
                    title = "Connect your server",
                    subtitle = "MediaMagnet needs the address of your server before it can download anything.",
                ) {
                    Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Open settings")
                    }
                }
            }
        }

        item {
            SectionCard(title = "New download", subtitle = "Paste a video, gallery or track URL") {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL") },
                    singleLine = true,
                    placeholder = { Text("https://…") },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    backends.forEach { (value, label) ->
                        ToggleButton(
                            checked = backend == value,
                            onCheckedChange = { backend = value },
                        ) {
                            Text(label)
                        }
                    }
                }

                TextButton(onClick = { advanced = !advanced }) {
                    Icon(Icons.Rounded.Tune, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(if (advanced) "Hide advanced options" else "Advanced options")
                }

                AnimatedVisibility(advanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Overrides for this job only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = draft.userAgent,
                            onValueChange = { updateDraft(draft.copy(userAgent = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("User agent") },
                            singleLine = true,
                        )
                        SwitchRow("Preserve source modification time", draft.preserveMtime) {
                            updateDraft(draft.copy(preserveMtime = it))
                        }

                        if (backend == null || backend == "yt-dlp") {
                            Text("yt-dlp", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = draft.ytFormat,
                                onValueChange = { updateDraft(draft.copy(ytFormat = it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Format selector") },
                                singleLine = true,
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
                                        onCheckedChange = {
                                            updateDraft(draft.copy(ytRecodeFormat = format))
                                        },
                                    ) {
                                        Text(format.ifBlank { "none" })
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = draft.ytOutputTemplate,
                                onValueChange = { updateDraft(draft.copy(ytOutputTemplate = it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Output template") },
                                singleLine = true,
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
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Global arguments") },
                                minLines = 2,
                            )
                            SiteArgsEditor(
                                label = "yt-dlp",
                                args = draft.ytSiteArgs,
                                onChange = { updateDraft(draft.copy(ytSiteArgs = it)) },
                            )
                        }

                        if (backend == null || backend == "gallery-dl") {
                            Text("gallery-dl", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = draft.gdlGlobalArgs,
                                onValueChange = { updateDraft(draft.copy(gdlGlobalArgs = it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Global arguments") },
                                minLines = 2,
                            )
                            SiteArgsEditor(
                                label = "gallery-dl",
                                args = draft.gdlSiteArgs,
                                onChange = { updateDraft(draft.copy(gdlSiteArgs = it)) },
                            )
                        }

                        if (backend == null || backend == "spotdl") {
                            Text("spotdl", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = draft.spotdlFormat,
                                onValueChange = { updateDraft(draft.copy(spotdlFormat = it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Audio format") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = draft.spotdlBitrate,
                                onValueChange = { updateDraft(draft.copy(spotdlBitrate = it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Bitrate") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = draft.spotdlGlobalArgs,
                                onValueChange = { updateDraft(draft.copy(spotdlGlobalArgs = it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Global arguments") },
                                minLines = 2,
                            )
                        }

                        Text("Cookies", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = cookieDomain,
                            onValueChange = { cookieDomain = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Job cookie domain (optional)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = cookieContent,
                            onValueChange = { cookieContent = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Job cookie content (Netscape or JSON)") },
                            minLines = 2,
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.startDownload(
                            url,
                            backend,
                            jobOverrides(draft, backend),
                            cookieDomain,
                            cookieContent,
                        )
                        url = ""
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Start download")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Jobs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (sorted.any { it.isRunning || it.isPaused }) {
                    TextButton(onClick = viewModel::cancelAll) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Cancel all")
                    }
                }
            }
        }

        if (sorted.isEmpty()) {
            item {
                Text(
                    text = "No downloads yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        items(sorted, key = { it.id }) { job ->
            JobCard(
                job = job,
                onPause = { viewModel.pause(job.id) },
                onResume = { viewModel.resume(job.id) },
                onCancel = { viewModel.cancel(job.id) },
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun JobCard(
    job: JobUi,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val host = runCatching { Uri.parse(job.url).host }.getOrNull()
        ?: job.url.take(48)
    val created = if (job.createdAtMs > 0) formatTime(job.createdAtMs) else null

    SectionCard(
        title = host,
        subtitle = created,
        trailing = { StatePill(job.state) },
    ) {
        Text(
            text = job.url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (job.isRunning || job.isPaused) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val progress = job.progress
                if (progress != null) {
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (job.isPaused) {
                OutlinedButton(onClick = onResume) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text("Resume")
                }
            }
            if (job.isRunning) {
                OutlinedButton(onClick = onPause) {
                    Icon(Icons.Rounded.Pause, contentDescription = null)
                    Text("Pause")
                }
            }
            if (job.isRunning || job.isPaused) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ms))

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
