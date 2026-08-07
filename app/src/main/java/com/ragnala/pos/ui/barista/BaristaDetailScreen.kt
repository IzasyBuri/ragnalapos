package com.ragnala.pos.ui.barista

import com.ragnala.pos.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ragnala.pos.data.db.OrderEntity
import com.ragnala.pos.data.db.OrderItemEntity
import com.ragnala.pos.domain.OrderStatus
import com.ragnala.pos.ui.customer.formatRupiah
import com.ragnala.pos.ui.receipt.BluetoothReceiptPrinter
import com.ragnala.pos.ui.receipt.ReceiptScreen
import com.ragnala.pos.ui.receipt.printReceipt
import com.ragnala.pos.ui.theme.CoinDrop
import com.ragnala.pos.ui.theme.OrderProgressBar
import com.ragnala.pos.ui.theme.SteamWisp
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import java.io.File

/**
 * Barista Mode â€” slice 2: order detail + pay-cash.
 * Tap a queue order -> here. The barista is already authenticated by entering
 * Barista Mode, so no extra PIN is prompted at payment/void time.
 * When PAID or later, a receipt view is available.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaristaDetailScreen(
    viewModel: BaristaDetailViewModel,
    onBack: () -> Unit,
) {
    var showReceipt by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showBtPicker by remember { mutableStateOf(false) }
    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) showBtPicker = true
        else viewModel.showError("Bluetooth permission required to print")
    }
    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                persistImageFile(context, uri, "receipt_logo.png")?.let { path ->
                    viewModel.setLogoPath(path)
                } ?: run { viewModel.showError("Could not save logo") }
            }
        }
    }
    val order by viewModel.order.collectAsState(initial = null)
    val items by viewModel.items.collectAsState()
    val paid by viewModel.paid.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val logoPath by viewModel.logoPath.collectAsState()
    val logoBitmap = remember(logoPath) {
        logoPath?.let(BitmapFactory::decodeFile)
    }
    val qrisPath by viewModel.qrisPath.collectAsState()
    val qrisBitmap = remember(qrisPath) {
        qrisPath?.let(BitmapFactory::decodeFile)
    }

    val currentOrder = order
    if (showReceipt && currentOrder != null) {
        ReceiptScreen(
            order = currentOrder,
            items = items,
            payments = payments,
            storeName = storeName,
            onBack = { showReceipt = false },
            showBtPicker = showBtPicker,
            onRequestBluetooth = {
                btPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
            },
            onBtPickerDismiss = { showBtPicker = false },
            onPrint = {
                printReceipt(
                    context = context,
                    order = currentOrder,
                    items = items,
                    payments = payments,
                    storeName = storeName,
                    logo = logoBitmap,
                )
            },
            onPickLogo = {
                logoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            hasLogo = logoPath != null,
            onRemoveLogo = {
                logoPath?.let { path -> runCatching { File(path).delete() } }
                viewModel.removeLogo()
            },
            onBluetoothPrint = { device ->
                coroutineScope.launch {
                    viewModel.showError("Printing to ${device.name ?: device.address}â€¦")
                    try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            BluetoothReceiptPrinter.print(
                                device = device,
                                order = currentOrder,
                                items = items,
                                payments = payments,
                                storeName = storeName,
                                logo = logoBitmap,
                            )
                        }
                        viewModel.showError("Printed to ${device.name ?: device.address}")
                    } catch (e: Exception) {
                        viewModel.showError("Print failed: ${e.message}")
                    }
                }
            },
        )
        return
    }

    val error by viewModel.error.collectAsState()

    var tenderedRaw by remember { mutableStateOf("") }   // raw digits only
    var nonCashMode by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf("QRIS") }
    // Cancel (WAITING_PAYMENT) and Void (PAID) dialogs.
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var showVoidDialog by remember { mutableStateOf(false) }
    var voidReason by remember { mutableStateOf("") }

    val o = order
    val total = o?.total ?: 0L
    val tendered = tenderedRaw.toLongOrNull() ?: 0L

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.barista_back))
                }
                Text(
                    text = stringResource(R.string.barista_order_title, o?.orderNumber ?: ""),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        if (o == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.barista_order_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val label = o.customerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.barista_customer)
                Text(stringResource(R.string.barista_for, label), style = MaterialTheme.typography.titleMedium)
            }
            items(items, key = { it.id }) { line ->
                OrderLineRow(line = line)
            }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    SummaryLine(stringResource(R.string.barista_subtotal), formatRupiah(o.subtotal))
                    SummaryLine(stringResource(R.string.barista_service_charge), formatRupiah(o.serviceCharge))
                    SummaryLine(stringResource(R.string.barista_tax), formatRupiah(o.tax))
                    SummaryLine(stringResource(R.string.barista_total), formatRupiah(o.total), bold = true)
                }
            }
        }

        // Bottom action panel â€” driven by live order status
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Cozy quest progress: Order(1) â†’ Paid(2)
                val step = when (o.status) {
                    OrderStatus.WAITING_PAYMENT -> 1
                    OrderStatus.PAID, OrderStatus.FULFILLED -> 2
                    else -> 2
                }
                OrderProgressBar(currentStep = step, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                when (o.status) {
                    OrderStatus.WAITING_PAYMENT -> {
                        // Fixed "Rp" prefix inside the box; field shows thousand separators.
                        val tenderedTooLow = tenderedRaw.isNotEmpty() && tendered < total
                        // Audit M8: quick-tender shortcuts (Exact / common denominations).
                        val exactText = stringResource(R.string.barista_exact)
                        val quickTenders = listOf(
                            exactText to total,
                            "Rp50.000" to 50_000L,
                            "Rp100.000" to 100_000L,
                        ).filter { it.second >= total }
                        if (quickTenders.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                quickTenders.forEach { (label, amount) ->
                                    OutlinedButton(
                                        onClick = { tenderedRaw = amount.toString() },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = formatThousands(tenderedRaw),
                                onValueChange = { tenderedRaw = it.filter { c -> c.isDigit() } },
                                label = { Text(stringResource(R.string.barista_cash_tendered)) },
                                leadingIcon = { Text("Rp", style = MaterialTheme.typography.bodyLarge) },
                                placeholder = { Text("0") },
                                isError = tenderedTooLow,
                                supportingText = if (tenderedTooLow) {
                                    { Text(stringResource(R.string.barista_amount_less_than, formatRupiah(total))) }
                                } else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        // The barista is already authenticated (Barista Mode entry); no PIN is prompted
                        // at payment time. The service still records pinVerified = true (session gate).
                        val tenderedLessThanTotal = stringResource(
                            R.string.barista_tendered_less_than,
                            formatRupiah(tendered),
                            formatRupiah(total),
                        )
                        Button(
                            onClick = {
                                if (tendered < total) {
                                    viewModel.showError(tenderedLessThanTotal)
                                } else {
                                    viewModel.payCash(
                                        tendered = tendered,
                                        userLabel = "Barista",
                                        now = System.currentTimeMillis(),
                                    )
                                }
                            },
                            enabled = tenderedRaw.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Text(stringResource(R.string.barista_pay_cash, formatRupiah(tendered)))
                        }
                        TextButton(
                            onClick = { nonCashMode = !nonCashMode },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(
                                if (nonCashMode) {
                                    stringResource(R.string.barista_hide_card_transfer)
                                } else {
                                    stringResource(R.string.barista_pay_qris_card_transfer)
                                },
                            )
                        }
                        if (nonCashMode) {
                            // Audit M7: QRIS, Debit card, Credit card, Bank transfer.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                NON_CASH_METHODS.forEach { m ->
                                    FilterChip(
                                        selected = selectedMethod == m,
                                        onClick = { selectedMethod = m },
                                        label = { Text(paymentMethodLabel(m)) },
                                    )
                                }
                            }
                            // Owner-configured QRIS image (set in Management) â€” show it for the customer to scan.
                            if (selectedMethod == "QRIS") {
                                qrisBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = stringResource(R.string.barista_qris_payment_code),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                    )
                                } ?: Text(
                                    stringResource(R.string.barista_no_qris_image),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.payNonCash(
                                        method = selectedMethod,
                                        userLabel = "Barista",
                                        now = System.currentTimeMillis(),
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) {
                                Text(stringResource(R.string.barista_confirm_payment, paymentMethodLabel(selectedMethod), formatRupiah(total)))
                            }
                        }
                        TextButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(stringResource(R.string.barista_cancel_order), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    OrderStatus.PAID -> {
                        if (paid) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CoinDrop(modifier = Modifier.size(40.dp))
                                Text(
                                    stringResource(R.string.barista_paid_change, formatRupiah((tendered.coerceAtLeast(total)) - total)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                                )
                            }
                        } else {
                            Text(
                                stringResource(R.string.barista_paid),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        // Drink handed over but not yet marked done
                        StatusButton(stringResource(R.string.barista_mark_fulfilled), onClick = {
                            viewModel.transitionTo(OrderStatus.FULFILLED, System.currentTimeMillis())
                        })
                        TextButton(
                            onClick = { showVoidDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(stringResource(R.string.barista_void_order), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    OrderStatus.FULFILLED -> {
                        Text(
                            stringResource(R.string.barista_fulfilled),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    else -> {
                        Text(
                            stringResource(R.string.barista_status_with, orderStatusLabel(o.status)),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                if (o.status != OrderStatus.WAITING_PAYMENT) {
                    TextButton(onClick = { showReceipt = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.barista_view_receipt))
                    }
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.barista_back_to_queue))
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.barista_cancel_order_title, o?.orderNumber ?: "")) },
            text = {
                OutlinedTextField(
                    value = cancelReason,
                    onValueChange = { cancelReason = it },
                    label = { Text(stringResource(R.string.barista_reason_required)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = cancelReason.isNotBlank(),
                    onClick = {
                        viewModel.cancelOrder(
                            reason = cancelReason,
                            userLabel = "Barista",
                            now = System.currentTimeMillis(),
                        )
                        showCancelDialog = false
                        cancelReason = ""
                    },
                ) { Text(stringResource(R.string.barista_cancel_order)) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text(stringResource(R.string.barista_keep_order)) }
            },
        )
    }

    if (showVoidDialog) {
        AlertDialog(
            onDismissRequest = { showVoidDialog = false },
            title = { Text(stringResource(R.string.barista_void_order_title, o?.orderNumber ?: "")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text(stringResource(R.string.barista_reason_required)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = voidReason.isNotBlank(),
                    onClick = {
                        viewModel.voidOrder(
                            reason = voidReason,
                            userLabel = "Barista",
                            now = System.currentTimeMillis(),
                        )
                        showVoidDialog = false
                        voidReason = ""
                    },
                ) { Text(stringResource(R.string.barista_void_order)) }
            },
            dismissButton = {
                TextButton(onClick = { showVoidDialog = false }) { Text(stringResource(R.string.barista_keep_order)) }
            },
        )
    }
}

/** Master list of supported non-cash payment methods. Stable order for UI chips (M7). */
private val NON_CASH_METHODS = listOf("QRIS", "DEBIT", "CREDIT_CARD", "BANK_TRANSFER")

/** Human-readable label for a payment method enum value (M7). */
private fun paymentMethodLabel(method: String): String = when (method) {
    "QRIS" -> "QRIS"
    "DEBIT" -> "Debit card"
    "CREDIT_CARD" -> "Credit card"
    "BANK_TRANSFER" -> "Bank transfer"
    else -> method
}

@Composable
private fun StatusButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun OrderLineRow(line: OrderItemEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${line.quantity}x ${line.productName}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(formatRupiah(line.unitPrice * line.quantity), style = MaterialTheme.typography.bodyLarge)
        }
        line.note?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            else MaterialTheme.typography.bodyMedium,
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            else MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Format a raw digit string with "." thousand separators: "20000" -> "20.000". */
private fun formatThousands(rawDigits: String): String {
    if (rawDigits.isEmpty()) return ""
    return rawDigits.reversed().chunked(3).joinToString(".").reversed()
}

/** Persist a picked image to app files under [fileName] and return its path, or null on failure. */
private suspend fun persistImageFile(
    context: android.content.Context,
    uri: android.net.Uri,
    fileName: String,
): String? = try {
    val file = java.io.File(context.filesDir, fileName)
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        if (bmp != null) {
            java.io.FileOutputStream(file).use { out ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        file.absolutePath
    }
} catch (_: Exception) {
    null
}
