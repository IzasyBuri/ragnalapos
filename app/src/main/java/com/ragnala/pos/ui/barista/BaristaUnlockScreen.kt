package com.ragnala.pos.ui.barista

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Shown the first time Barista Mode is opened in a session (or whenever the
 * session is locked). Enter the barista PIN once to unlock the whole session.
 * Owners may disable the PIN for the current day via the toggle (owner-PIN gated).
 */
@Composable
fun BaristaUnlockScreen(
    viewModel: BaristaUnlockViewModel,
    onUnlocked: () -> Unit,
) {
    val pin by viewModel.pin.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val pinDisabledToday by viewModel.pinDisabledToday.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()

    // Owner-PIN sheet state for the disable toggle
    var ownerPin by remember { mutableStateOf("") }
    var ownerSheetOpen by remember { mutableStateOf(false) }

    // Once unlocked, hand control back to the host (which navigates into the queue).
    LaunchedEffect(unlocked) {
        if (unlocked) onUnlocked()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.barista_mode),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = stringResource(R.string.barista_enter_pin_to_continue),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = viewModel::onPinChange,
            label = { Text(stringResource(R.string.barista_pin)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.tryUnlock() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.barista_enter_barista_mode), fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Owner convenience: disable PIN for today
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.barista_disable_pin_today),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (pinDisabledToday) {
                        stringResource(R.string.barista_pin_off_today)
                    } else {
                        stringResource(R.string.barista_require_pin_session)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = pinDisabledToday,
                onCheckedChange = {
                    if (pinDisabledToday) {
                        // turning OFF the exemption -> ask owner PIN
                        ownerSheetOpen = true
                    } else {
                        // turning ON the exemption -> ask owner PIN
                        ownerSheetOpen = true
                    }
                },
            )
        }

        if (ownerSheetOpen) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = ownerPin,
                onValueChange = { ownerPin = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text(stringResource(R.string.barista_owner_pin_required)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    ownerPin = ""
                    ownerSheetOpen = false
                    viewModel.clearError()
                }) { Text(stringResource(R.string.barista_cancel)) }
                Button(onClick = {
                    viewModel.togglePinDisabledForToday(ownerPin)
                    ownerPin = ""
                    ownerSheetOpen = false
                }) { Text(stringResource(R.string.barista_confirm)) }
            }
        }
    }
}
