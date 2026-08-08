package com.ragnala.pos.ui.customer

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ragnala.pos.ui.components.RagnalaPrimaryButton
import com.ragnala.pos.ui.theme.RagnalaSpacing

@Composable
fun NameScreen(onContinue: (String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val canContinue = name.trim().isNotBlank()
    Column(modifier = Modifier.imePadding().padding(horizontal = RagnalaSpacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = onBack, modifier = Modifier.padding(vertical = RagnalaSpacing.xs)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cust_back))
            }
        }
        Column(modifier = Modifier.widthIn(max = 560.dp).padding(top = RagnalaSpacing.xxl), verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.md)) {
            androidx.compose.material3.Text("Your name for the order", style = MaterialTheme.typography.headlineSmall)
            androidx.compose.material3.Text("We'll use this when your order is ready.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { androidx.compose.material3.Text(stringResource(R.string.cust_name_field)) },
                placeholder = { androidx.compose.material3.Text(stringResource(R.string.cust_name_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary),
            )
            RagnalaPrimaryButton("Continue", { onContinue(name.trim()) }, enabled = canContinue, modifier = Modifier.fillMaxWidth().padding(top = RagnalaSpacing.xs).heightIn(min = 52.dp))
        }
    }
}
