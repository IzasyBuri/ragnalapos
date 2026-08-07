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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ragnala.pos.data.db.IngredientEntity
import com.ragnala.pos.ui.customer.formatRupiah

/** Inventory (PRD Â§9): ingredients, low-stock surfacing, stock adjustments, CRUD. */
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
) {
    val ingredients by viewModel.ingredients.collectAsState(initial = emptyList())
    val lowStock by viewModel.lowStock.collectAsState(initial = emptyList())
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<IngredientEntity?>(null) }
    var adjusting by remember { mutableStateOf<IngredientEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mgmt_back))
                }
                Text(stringResource(R.string.mgmt_inventory_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
        }

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            viewModel.clearMessage()
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }

        if (lowStock.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.mgmt_low_stock), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    lowStock.take(5).forEach { ing ->
                        Text("â€¢ ${ing.name} (${ing.currentStock} ${ing.unit})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        if (ingredients.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.mgmt_no_ingredients), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ingredients, key = { it.id }) { ing ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium,
                        onClick = { editing = ing },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ing.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text("${ing.currentStock} ${ing.unit}", style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("min ${ing.minStock}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text("cost ${formatRupiah(ing.costPerUnit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { adjusting = ing }, modifier = Modifier.padding(top = 4.dp)) {
                                Text(stringResource(R.string.mgmt_adjust_stock))
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { editing = null; showForm = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(stringResource(R.string.mgmt_add_ingredient))
        }
    }

    if (showForm) {
        IngredientFormDialog(
            title = if (editing == null) stringResource(R.string.mgmt_add_ingredient) else stringResource(R.string.mgmt_edit_ingredient),
            initial = editing,
            onDismiss = { showForm = false; editing = null },
            onSave = { id, name, unit, current, min, cost ->
                viewModel.saveIngredient(id, name, unit, current, min, cost)
                showForm = false
                editing = null
            },
            onDelete = if (editing != null) {
                { ingredient ->
                    viewModel.deleteIngredient(ingredient)
                    showForm = false
                    editing = null
                }
            } else null,
        )
    }

    adjusting?.let { ingredient ->
        AdjustStockDialog(
            ingredient = ingredient,
            onDismiss = { adjusting = null },
            onAdjust = { delta, reason ->
                viewModel.adjustStock(ingredient.id, delta, reason)
                adjusting = null
            },
        )
    }
}

@Composable
private fun IngredientFormDialog(
    title: String,
    initial: IngredientEntity?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, Double, Double, Long) -> Unit,
    onDelete: ((IngredientEntity) -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var current by remember { mutableStateOf(initial?.currentStock?.toString() ?: "0") }
    var min by remember { mutableStateOf(initial?.minStock?.toString() ?: "0") }
    var cost by remember { mutableStateOf(initial?.costPerUnit?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.mgmt_ingredient_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text(stringResource(R.string.mgmt_ingredient_unit)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = current, onValueChange = { current = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.mgmt_current_stock)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = min, onValueChange = { min = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.mgmt_min_stock)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cost, onValueChange = { cost = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.mgmt_cost_per_unit)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(initial?.id, name, unit, current.toDoubleOrNull() ?: 0.0, min.toDoubleOrNull() ?: 0.0, cost.toLongOrNull() ?: 0L)
                },
            ) { Text(stringResource(R.string.mgmt_save)) }
        },
        dismissButton = {
            Row {
                onDelete?.let { del ->
                    initial?.let { ing ->
                        TextButton(onClick = { del(ing) }) { Text(stringResource(R.string.mgmt_delete), color = MaterialTheme.colorScheme.error) }
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.mgmt_cancel)) }
            }
        },
    )
}

@Composable
private fun AdjustStockDialog(
    ingredient: IngredientEntity,
    onDismiss: () -> Unit,
    onAdjust: (Double, String) -> Unit,
) {
    var delta by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mgmt_adjust_stock_title, ingredient.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = delta,
                    onValueChange = { delta = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text(stringResource(R.string.mgmt_delta_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.mgmt_reason_required)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = reason.isNotBlank() && delta.toDoubleOrNull() != null,
                onClick = { onAdjust(delta.toDouble(), reason) },
            ) { Text(stringResource(R.string.mgmt_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.mgmt_cancel)) }
        },
    )
}
