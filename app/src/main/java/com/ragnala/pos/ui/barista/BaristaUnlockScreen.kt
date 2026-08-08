package com.ragnala.pos.ui.barista

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ragnala.pos.ui.components.RagnalaCard
import com.ragnala.pos.ui.components.RagnalaPrimaryButton
import com.ragnala.pos.ui.components.RagnalaSecondaryButton
import com.ragnala.pos.ui.components.RagnalaTopBar
import com.ragnala.pos.ui.theme.RagnalaSpacing

@Composable
fun BaristaUnlockScreen(viewModel: BaristaUnlockViewModel, onUnlocked: () -> Unit) {
    val pin by viewModel.pin.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val pinDisabledToday by viewModel.pinDisabledToday.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    var ownerPin by remember { mutableStateOf("") }
    var ownerOptionsOpen by remember { mutableStateOf(false) }

    LaunchedEffect(unlocked) { if (unlocked) onUnlocked() }

    Column {
        RagnalaTopBar(title = "Barista access")
        BoxWithConstraints {
            Column(
                modifier = Modifier.fillMaxWidth().padding(RagnalaSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.md),
            ) {
                Column(modifier = Modifier.widthIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.md)) {
                    Text("Ragnala POS", style = MaterialTheme.typography.headlineSmall)
                    Text("Enter your staff PIN to continue.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = pin,
                        onValueChange = viewModel::onPinChange,
                        label = { Text(stringResource(R.string.barista_pin)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = error != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                    RagnalaPrimaryButton("Unlock", { viewModel.tryUnlock() }, enabled = pin.length >= 4, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp))
                    RagnalaCard(contentPadding = RagnalaSpacing.md) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Owner options", style = MaterialTheme.typography.titleMedium)
                                Text(if (pinDisabledToday) stringResource(R.string.barista_pin_off_today) else stringResource(R.string.barista_require_pin_session), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = pinDisabledToday, onCheckedChange = { ownerOptionsOpen = true })
                        }
                        if (ownerOptionsOpen) {
                            OutlinedTextField(value = ownerPin, onValueChange = { ownerPin = it.filter(Char::isDigit).take(6) }, label = { Text(stringResource(R.string.barista_owner_pin_required)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth().padding(top = RagnalaSpacing.sm))
                            Row(Modifier.fillMaxWidth().padding(top = RagnalaSpacing.sm), horizontalArrangement = Arrangement.spacedBy(RagnalaSpacing.sm)) {
                                RagnalaSecondaryButton("Cancel", { ownerPin = ""; ownerOptionsOpen = false; viewModel.clearError() }, modifier = Modifier.weight(1f))
                                RagnalaPrimaryButton("Verify", { viewModel.togglePinDisabledForToday(ownerPin); ownerPin = "" }, enabled = ownerPin.length >= 4, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
