package com.ragnala.pos.ui.management

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import kotlinx.coroutines.launch

/**
 * Management (PRD Â§9): owner-gated settings â€” PIN changes and store config.
 * Owner PIN dev default 9999.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementScreen(
    viewModel: ManagementViewModel,
    onBack: () -> Unit,
    onProductsClick: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
) {
    val ownerVerified by viewModel.ownerVerified.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    var ownerPin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mgmt_back))
                }
                Text(stringResource(R.string.mgmt_management_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                viewModel.clearMessage()
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            if (!ownerVerified) {
                OutlinedTextField(
                    value = ownerPin,
                    onValueChange = { ownerPin = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text(stringResource(R.string.mgmt_owner_pin)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { viewModel.verifyOwner(ownerPin) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.mgmt_unlock)) }
                return@Column
            }

            // Owner verified â€” show settings
            Surface(
                onClick = onProductsClick,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.mgmt_menu_products), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.mgmt_menu_products_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            ManagementMenuRow(stringResource(R.string.mgmt_inventory_title), stringResource(R.string.mgmt_inventory_desc), onInventoryClick)
            ManagementMenuRow(stringResource(R.string.mgmt_expenses_title), stringResource(R.string.mgmt_expenses_desc), onExpensesClick)
            ManagementMenuRow(stringResource(R.string.mgmt_reports_title), stringResource(R.string.mgmt_reports_desc), onReportsClick)
            ManagementMenuRow(stringResource(R.string.mgmt_backup_title), stringResource(R.string.mgmt_backup_desc), onBackupClick)
            PinChangeRow(label = stringResource(R.string.mgmt_barista_pin), onSave = { viewModel.changeBaristaPin(it) })
            PinChangeRow(label = stringResource(R.string.mgmt_owner_pin), onSave = { viewModel.changeOwnerPin(it) })
            StoreSettings(viewModel = viewModel)
            QrisSettings(viewModel = viewModel)
        }
    }
}

@Composable
private fun ManagementMenuRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PinChangeRow(label: String, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
            label = { Text(stringResource(R.string.mgmt_new_label_digits, label)) },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = { if (pin.length in 4..6) { onSave(pin); pin = "" } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.mgmt_save_label, label)) }
    }
}

@Composable
private fun QrisSettings(viewModel: ManagementViewModel) {
    val qrisPath by viewModel.qrisImagePath.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val path = persistManagementImage(context, uri, "qris_image.png")
                viewModel.setQrisImagePath(path)
            }
        }
    }

    Text(stringResource(R.string.mgmt_qris_payment_image), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(R.string.mgmt_qris_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (qrisPath != null) {
        val bmp = remember(qrisPath) { qrisPath?.let(android.graphics.BitmapFactory::decodeFile) }
        bmp?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.mgmt_qris_payment_code),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp),
            )
        }
    }
    OutlinedButton(
        onClick = { picker.launch("image/*") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (qrisPath != null) stringResource(R.string.mgmt_change_qris_image) else stringResource(R.string.mgmt_upload_qris_image))
    }
    if (qrisPath != null) {
        TextButton(
            onClick = { viewModel.setQrisImagePath(null) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.mgmt_remove_qris_image), color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Persist a picked management image to app files and return its absolute path. */
private suspend fun persistManagementImage(
    context: android.content.Context,
    uri: android.net.Uri,
    fileName: String,
): String? = runCatching {
    val dir = context.filesDir
    val target = java.io.File(dir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    }
    target.absolutePath
}.getOrNull()

@Composable
private fun StoreSettings(viewModel: ManagementViewModel) {
    val storeName by viewModel.storeName.collectAsState()
    val sc by viewModel.scPercent.collectAsState()
    val tax by viewModel.taxPercent.collectAsState()

    var name by remember(storeName) { mutableStateOf(storeName) }
    var scText by remember(sc) { mutableStateOf(sc.toString()) }
    var taxText by remember(tax) { mutableStateOf(tax.toString()) }

    Text(stringResource(R.string.mgmt_store_settings), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = name,
        onValueChange = { name = it; viewModel.setStoreName(it) },
        label = { Text(stringResource(R.string.mgmt_store_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = scText,
        onValueChange = { scText = it; it.toDoubleOrNull()?.let { v -> viewModel.setScPercent(v) } },
        label = { Text(stringResource(R.string.mgmt_service_charge_pct)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = taxText,
        onValueChange = { taxText = it; it.toDoubleOrNull()?.let { v -> viewModel.setTaxPercent(v) } },
        label = { Text(stringResource(R.string.mgmt_tax_pct)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(
        onClick = { viewModel.saveStoreSettings() },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.mgmt_save_settings)) }
}
