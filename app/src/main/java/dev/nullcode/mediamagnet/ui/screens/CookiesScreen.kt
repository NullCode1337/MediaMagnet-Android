package dev.nullcode.mediamagnet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nullcode.mediamagnet.ui.AppViewModel
import dev.nullcode.mediamagnet.ui.components.SectionCard

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CookiesScreen(viewModel: AppViewModel) {
    val cookies by viewModel.cookies.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCookies()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = "Cookie profiles",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::loadCookies) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reload")
                }
            }
        }

        item {
            SectionCard(
                title = "How cookies work",
                subtitle = "The server applies a profile when its domain matches the download URL. Content accepts Netscape text or JSON; file paths are read on the server.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { adding = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Add cookie", modifier = Modifier.padding(start = 4.dp))
                    }
                    if (cookies.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) {
                            Text("Delete all")
                        }
                    }
                }
            }
        }

        if (cookies.isEmpty()) {
            item {
                Text(
                    text = "No cookie profiles saved on the server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        items(cookies.entries.toList(), key = { it.key }) { (domain, path) ->
            SectionCard(title = domain, subtitle = path) {
                TextButton(
                    onClick = { viewModel.deleteCookie(domain) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Text("Delete", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }

    if (adding) {
        AddCookieDialog(
            onDismiss = { adding = false },
            onSave = { domain, type, value ->
                viewModel.saveCookie(domain, type, value)
                adding = false
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all cookie profiles?") },
            text = { Text("This removes every cookie file from the server's cookie directory.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCookies()
                    confirmClear = false
                }) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddCookieDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var domain by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Content") }
    var value by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add cookie profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Content", "FilePath").forEach { option ->
                        ToggleButton(
                            checked = type == option,
                            onCheckedChange = { type = option },
                        ) {
                            Text(option)
                        }
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (type == "Content") "Netscape text or JSON" else "Server file path") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(domain, type, value) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
