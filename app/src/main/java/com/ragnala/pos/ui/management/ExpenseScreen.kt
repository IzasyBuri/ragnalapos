package com.ragnala.pos.ui.management

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.ragnala.pos.data.db.ExpenseEntity
import com.ragnala.pos.ui.customer.formatRupiah
import com.ragnala.pos.ui.components.RagnalaEmptyState
import com.ragnala.pos.ui.components.RagnalaMoneySize
import com.ragnala.pos.ui.components.RagnalaMoneyText
import com.ragnala.pos.ui.components.RagnalaPrimaryButton
import com.ragnala.pos.ui.theme.RagnalaRadius
import com.ragnala.pos.ui.theme.RagnalaSpacing

/** Expenses (PRD Â§9): list this month's expenses, add, and remove. */
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit,
) {
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mgmt_back)) }
                Text(stringResource(R.string.mgmt_expenses_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
        }

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            viewModel.clearMessage()
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }

        val total = expenses.sumOf { it.amount }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(RagnalaRadius.card), modifier = Modifier.fillMaxWidth().padding(horizontal = RagnalaSpacing.md, vertical = RagnalaSpacing.sm)) {
            Column(Modifier.padding(RagnalaSpacing.md)) {
                Text("Total expenses", style = MaterialTheme.typography.titleMedium)
                RagnalaMoneyText(total, size = RagnalaMoneySize.Large, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RagnalaEmptyState("No expenses yet", "Record store spending when it happens.", Modifier.fillMaxWidth())
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(expenses, key = { it.id }) { e ->
                    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = androidx.compose.foundation.shape.RoundedCornerShape(RagnalaRadius.card), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(RagnalaSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(e.category, style = MaterialTheme.typography.titleMedium)
                                e.note.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            RagnalaMoneyText(e.amount, size = RagnalaMoneySize.Medium)
                            TextButton(onClick = { viewModel.deleteExpense(e) }) {
                                Text(stringResource(R.string.mgmt_remove), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        RagnalaPrimaryButton(stringResource(R.string.mgmt_add_expense), { showAdd = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp))
    }

    if (showAdd) {
        AddExpenseDialog(
            onDismiss = { showAdd = false },
            onAdd = { category, amount, note ->
                viewModel.addExpense(category, amount, note)
                showAdd = false
            },
        )
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long, String) -> Unit,
) {
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mgmt_add_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(stringResource(R.string.mgmt_category)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.mgmt_amount_rp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.mgmt_note)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = category.isNotBlank() && (amount.toLongOrNull() ?: 0L) > 0,
                onClick = { onAdd(category, amount.toLong(), note) },
            ) { Text(stringResource(R.string.mgmt_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.mgmt_cancel)) } },
    )
}
