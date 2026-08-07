package com.ragnala.pos.ui.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.bluetooth.BluetoothDevice
import com.ragnala.pos.data.db.OrderEntity
import com.ragnala.pos.data.db.OrderItemEntity
import com.ragnala.pos.data.db.PaymentEntity
import com.ragnala.pos.ui.customer.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Print-style receipt for a paid/completed order (PRD §9 / §14). On-screen view; printing is v3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    order: OrderEntity,
    items: List<OrderItemEntity>,
    payments: List<PaymentEntity>,
    storeName: String,
    onBack: () -> Unit,
    onPrint: () -> Unit = {},
    onPickLogo: () -> Unit = {},
    hasLogo: Boolean = false,
    onRemoveLogo: () -> Unit = {},
    onBluetoothPrint: (BluetoothDevice) -> Unit = {},
    showBtPicker: Boolean = false,
    onRequestBluetooth: () -> Unit = {},
    onBtPickerDismiss: () -> Unit = {},
) {
    var showLogoActions by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Receipt", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showLogoActions = true }) {
                    Text("Logo")
                }
                TextButton(onClick = onRequestBluetooth) {
                    Text("Bluetooth")
                }
                TextButton(onClick = onPrint) {
                    Text("Print")
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Order #${order.orderNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val ts = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                    .format(Date(order.createdAt))
                Text(ts, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                order.customerName?.takeIf { it.isNotBlank() }?.let {
                    Text("Customer: $it", style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                items.forEach { line ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${line.quantity}x ${line.productName}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(formatRupiah(line.unitPrice * line.quantity), style = MaterialTheme.typography.bodyMedium)
                    }
                    line.note?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ReceiptRow("Subtotal", formatRupiah(order.subtotal))
                ReceiptRow("Service charge", formatRupiah(order.serviceCharge))
                ReceiptRow("Tax", formatRupiah(order.tax))
                ReceiptRow("Total", formatRupiah(order.total), bold = true)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                payments.forEach { p ->
                    if (p.method == "CASH") {
                        ReceiptRow("Cash", formatRupiah(p.tendered ?: p.amount))
                        p.changeGiven?.let { ReceiptRow("Change", formatRupiah(it)) }
                    } else {
                        ReceiptRow(p.method, formatRupiah(p.amount))
                    }
                }

                Text(
                    "Thank you!",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (showBtPicker) {
            BluetoothDevicePickerDialog(
                onDismiss = onBtPickerDismiss,
                onSelect = { device ->
                    onBtPickerDismiss()
                    onBluetoothPrint(device)
                },
            )
        }

        if (showLogoActions) {
            AlertDialog(
                onDismissRequest = { showLogoActions = false },
                title = { Text("Receipt logo") },
                text = {
                    Text(
                        if (hasLogo) "A logo is currently saved for printed receipts."
                        else "No receipt logo is currently saved."
                    )
                },
                confirmButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showLogoActions = false
                                onPickLogo()
                            },
                        ) {
                            Text(if (hasLogo) "Change logo" else "Add logo")
                        }
                        if (hasLogo) {
                            TextButton(
                                onClick = {
                                    showLogoActions = false
                                    onRemoveLogo()
                                },
                            ) {
                                Text("Remove logo", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoActions = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun BluetoothDevicePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (BluetoothDevice) -> Unit,
) {
    val devices = remember { BluetoothReceiptPrinter.pairedDevices() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bluetooth printer") },
        text = {
            if (devices.isEmpty()) {
                Text("No paired Bluetooth devices. Pair a printer in Settings → Bluetooth first.")
            } else {
                Column {
                    devices.forEach { device ->
                        TextButton(onClick = { onSelect(device) }) {
                            Text(device.name ?: device.address)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ReceiptRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium)
        Text(value, style = if (bold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium)
    }
}
