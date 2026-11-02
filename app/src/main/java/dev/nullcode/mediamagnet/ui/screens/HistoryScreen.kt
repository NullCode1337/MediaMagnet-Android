package dev.nullcode.mediamagnet.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nullcode.mediamagnet.data.DownloadRecord
import dev.nullcode.mediamagnet.ui.AppViewModel
import dev.nullcode.mediamagnet.ui.components.SectionCard
import dev.nullcode.mediamagnet.ui.components.StatePill
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(viewModel: AppViewModel) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val jobs = remember(prefs.history) {
        prefs.history
            .groupBy { it.jobId.ifBlank { it.id } }
            .toList()
            .sortedByDescending { (_, records) -> records.maxOf { it.savedAtMs } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Download history", style = MaterialTheme.typography.titleMedium)
        }

        if (jobs.isEmpty()) {
            item {
                Text(
                    text = "Nothing has been saved on this phone yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        items(jobs, key = { it.first }) { (jobId, records) ->
            val dismissState = rememberSwipeToDismissBoxState()
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                onDismiss = { viewModel.removeHistory(jobId) },
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Remove from history",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                },
            ) {
                HistoryCard(records = records, onRemove = { viewModel.removeHistory(jobId) })
            }
        }
    }
}

@Composable
private fun HistoryCard(records: List<DownloadRecord>, onRemove: () -> Unit) {
    val url = records.firstOrNull { it.url.isNotBlank() }?.url.orEmpty()
    val host = runCatching { Uri.parse(url).host }.getOrNull()
        ?: url.take(48).ifBlank { records.first().name }
    val savedAt = records.maxOf { it.savedAtMs }

    SectionCard(
        title = host,
        subtitle = formatTime(savedAt),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatePill("finished")
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove from history")
                }
            }
        },
    ) {
        if (url.isNotBlank()) {
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${records.size} file${if (records.size == 1) "" else "s"} saved automatically",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatTime(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ms))
