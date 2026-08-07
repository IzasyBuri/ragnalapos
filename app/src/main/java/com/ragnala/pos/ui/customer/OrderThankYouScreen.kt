package com.ragnala.pos.ui.customer

import com.ragnala.pos.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Confirmation screen shown after a customer submits an order.
 * Shows a friendly "thank you" message with the order number,
 * and instructs the customer to show this screen to the barista.
 * Barista Mode opens separately (from the bottom nav or management flow).
 */
@Composable
fun OrderThankYouScreen(
    orderId: String,
    onBackToMenu: () -> Unit,
) {
    val orderNumber = orderId.split("-").firstOrNull()?.take(8)?.uppercase() ?: "â€”"
    Column(modifier = Modifier.fillMaxSize()) {
        // Header (simple, no back button â€” order submitted)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.cust_order_placed),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        // Central message
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Decorative circle with checkmark-like icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "âœ“",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Text(
                text = stringResource(R.string.cust_thank_you_body),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 24.dp),
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.cust_order_number_note, orderNumber),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center,
            )
        }

        // Bottom button
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Button(
                onClick = onBackToMenu,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.cust_back_to_menu), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
