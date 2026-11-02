package dev.nullcode.mediamagnet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nullcode.mediamagnet.data.ToolStatus
import dev.nullcode.mediamagnet.ui.AppViewModel
import dev.nullcode.mediamagnet.ui.components.SectionCard

@Composable
fun ToolsScreen(viewModel: AppViewModel) {
    val tools by viewModel.tools.collectAsStateWithLifecycle()
    val updating by viewModel.toolsUpdating.collectAsStateWithLifecycle()
    val update by viewModel.toolUpdate.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadTools()
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Download tools",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::loadTools) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("Reload", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        if (updating) {
            item { LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        items(tools?.tools.orEmpty(), key = { it.name }) { tool ->
            ToolCard(tool)
        }

        item {
            SectionCard(
                title = "Toolchain",
                subtitle = "Runtimes the server found for its download backends",
            ) {
                LabeledValue("Deno", tools?.deno ?: "not found")
                LabeledValue("ffmpeg", tools?.ffmpeg ?: "not found")
                Button(
                    onClick = viewModel::updateTools,
                    enabled = !updating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null)
                    Text("Update tools", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }

    if (update != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissToolUpdate,
            title = { Text("Tool update") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text("Updated: ${update?.updated?.joinToString().orEmpty().ifBlank { "none" }}")
                    Box(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = update?.output.orEmpty(),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissToolUpdate) { Text("Close") }
            },
        )
    }
}

@Composable
private fun ToolCard(tool: ToolStatus) {
    SectionCard(
        title = tool.name,
        subtitle = tool.version ?: tool.error ?: "version unknown",
        trailing = {
            val container = if (tool.available) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
            val content = if (tool.available) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
            Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
                Text(
                    text = if (tool.available) "AVAILABLE" else "MISSING",
                    style = MaterialTheme.typography.labelSmallEmphasized,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        },
    ) {
        LabeledValue("Source", tool.source ?: "—")
        LabeledValue("Program", tool.program ?: "—")
        if (tool.args.isNotEmpty()) {
            LabeledValue("Arguments", tool.args.joinToString(" "))
        }
        if (tool.error != null) {
            Text(
                text = tool.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.65f),
        )
    }
}
