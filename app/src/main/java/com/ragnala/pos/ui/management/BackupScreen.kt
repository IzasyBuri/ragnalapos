package com.ragnala.pos.ui.management

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Backup/Restore (PRD Â§9 Backup): archive unit = DB + images; restore is destructive + confirmed. */
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
) {
    val backups by viewModel.backups.collectAsState()
    val lastBackup by viewModel.lastBackup.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()
    var restoreTarget by remember { mutableStateOf<File?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mgmt_back)) }
                Text(stringResource(R.string.mgmt_backup_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
        }

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            viewModel.clearMessage()
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }

        lastBackup?.let {
            Text(stringResource(R.string.mgmt_last_backup, timestamp(it)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
        }

        Button(
            onClick = { viewModel.createBackup() },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
            }
            Text(stringResource(R.string.mgmt_create_backup_now))
        }

        Text(stringResource(R.string.mgmt_archives), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))

        if (backups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.mgmt_no_backups), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(backups, key = { it.name }) { file ->
                    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, style = MaterialTheme.typography.titleMedium)
                                Text(timestamp(file), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { restoreTarget = file }) {
                                Text(stringResource(R.string.mgmt_restore), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    restoreTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            title = { Text(stringResource(R.string.mgmt_restore_backup)) },
            text = {
                Text(stringResource(R.string.mgmt_restore_warning, file.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restore(file)
                    restoreTarget = null
                }) { Text(stringResource(R.string.mgmt_restore), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) { Text(stringResource(R.string.mgmt_cancel)) }
            },
        )
    }
}

private fun timestamp(file: File): String =
    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
