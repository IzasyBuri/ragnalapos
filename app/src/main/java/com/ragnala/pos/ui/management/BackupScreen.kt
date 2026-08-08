package com.ragnala.pos.ui.management

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ragnala.pos.ui.components.RagnalaCard
import com.ragnala.pos.ui.components.RagnalaEmptyState
import com.ragnala.pos.ui.components.RagnalaPrimaryButton
import com.ragnala.pos.ui.components.RagnalaSecondaryButton
import com.ragnala.pos.ui.components.RagnalaSectionHeader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(viewModel: BackupViewModel, onBack: () -> Unit) {
    val backups by viewModel.backups.collectAsState()
    val lastBackup by viewModel.lastBackup.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()
    var restoreTarget by remember { mutableStateOf<File?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.mgmt_back)) }
            Text("Backup & Restore", style = MaterialTheme.typography.headlineSmall)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            lastBackup?.let { Text(stringResource(R.string.mgmt_last_backup, timestamp(it)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            RagnalaSectionHeader("Backup data", subtitle = "Create a copy of your current Ragnala POS data.")
            RagnalaPrimaryButton(stringResource(R.string.mgmt_create_backup_now), { viewModel.createBackup() }, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 52.dp))
            RagnalaSectionHeader("Restore data", subtitle = "Restore Ragnala POS from an existing backup file.")
            if (backups.isEmpty()) RagnalaEmptyState("No backups yet", "Create a backup before restoring data.", Modifier.fillMaxWidth())
            else LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(backups, key = { it.name }) { file ->
                    RagnalaCard(contentPadding = 12.dp) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(file.name, style = MaterialTheme.typography.titleMedium); Text(timestamp(file), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; RagnalaSecondaryButton(stringResource(R.string.mgmt_restore), { restoreTarget = file }) } }
                }
            }
        }
    }
    restoreTarget?.let { file ->
        AlertDialog(onDismissRequest = { restoreTarget = null }, title = { Text(stringResource(R.string.mgmt_restore_backup)) }, text = { Text(stringResource(R.string.mgmt_restore_warning, file.name)) }, confirmButton = { RagnalaPrimaryButton(stringResource(R.string.mgmt_restore), { viewModel.restore(file); restoreTarget = null }, modifier = Modifier.padding(8.dp)) }, dismissButton = { RagnalaSecondaryButton(stringResource(R.string.mgmt_cancel), { restoreTarget = null }, modifier = Modifier.padding(8.dp)) })
    }
}

private fun timestamp(file: File): String = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
