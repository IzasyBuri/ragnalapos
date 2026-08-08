package com.ragnala.pos.ui.customer

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ragnala.pos.ui.components.RagnalaPrimaryButton
import com.ragnala.pos.ui.components.RagnalaMoneySize
import com.ragnala.pos.ui.theme.RagnalaRadius
import com.ragnala.pos.ui.theme.RagnalaSpacing

@Composable
fun OrderThankYouScreen(
    orderNumber: Long,
    customerName: String = "",
    onBackToMenu: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(RagnalaSpacing.lg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(104.dp)) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(24.dp).fillMaxSize())
        }
        Text(if (customerName.isBlank()) "Thank you." else "Thank you, $customerName.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = RagnalaSpacing.lg))
        Text("Your order is in.", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = RagnalaSpacing.xs))
        Text("ORDER NUMBER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = RagnalaSpacing.xxl))
        Text("#${orderNumber.toString().padStart(3, '0')}", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("Please hand the tablet back to our barista.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = RagnalaSpacing.md))
        Spacer(Modifier.height(RagnalaSpacing.xxl))
        RagnalaPrimaryButton("Back to menu", onBackToMenu, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp))
    }
}
