package com.ragnala.pos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ragnala.pos.ui.customer.formatRupiah
import com.ragnala.pos.ui.theme.DarkErrorContainer
import com.ragnala.pos.ui.theme.DarkSuccessContainer
import com.ragnala.pos.ui.theme.DarkWarningContainer
import com.ragnala.pos.ui.theme.MutedRedContainer
import com.ragnala.pos.ui.theme.NaturalGreenContainer
import com.ragnala.pos.ui.theme.RagnalaElevation
import com.ragnala.pos.ui.theme.RagnalaMoneyLarge
import com.ragnala.pos.ui.theme.RagnalaMoneyMedium
import com.ragnala.pos.ui.theme.RagnalaRadius
import com.ragnala.pos.ui.theme.RagnalaSpacing
import com.ragnala.pos.ui.theme.WarmAmberContainer

@Composable
fun RagnalaCard(
    modifier: Modifier = Modifier,
    border: Boolean = true,
    contentPadding: androidx.compose.ui.unit.Dp = RagnalaSpacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(RagnalaRadius.card),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = RagnalaElevation.none,
        shadowElevation = RagnalaElevation.subtle,
        border = if (border) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun RagnalaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(RagnalaRadius.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun RagnalaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(RagnalaRadius.button),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

enum class RagnalaBadgeTone { Neutral, Success, Warning, Error }

@Composable
fun RagnalaStatusBadge(
    text: String,
    tone: RagnalaBadgeTone = RagnalaBadgeTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val colors = badgeColors(tone)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(RagnalaRadius.smallControl),
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = RagnalaSpacing.xs, vertical = RagnalaSpacing.xxs),
        )
    }
}

@Composable
fun RagnalaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.xxs)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.let {
            Spacer(modifier = Modifier.width(RagnalaSpacing.md))
            it()
        }
    }
}

enum class RagnalaMoneySize { Large, Medium, Small }

@Composable
fun RagnalaMoneyText(
    amount: Long,
    modifier: Modifier = Modifier,
    size: RagnalaMoneySize = RagnalaMoneySize.Medium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = formatRupiah(amount),
        style = moneyStyle(size),
        color = color,
        modifier = modifier,
    )
}

@Composable
fun RagnalaEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    decoration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(RagnalaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.md),
    ) {
        decoration?.let {
            Surface(
                shape = RoundedCornerShape(RagnalaRadius.card),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { it() }
            }
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

@Composable
fun RagnalaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = RagnalaElevation.subtle,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .defaultMinSize(minHeight = 64.dp)
                .padding(horizontal = RagnalaSpacing.md, vertical = RagnalaSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.let {
                it()
                Spacer(modifier = Modifier.width(RagnalaSpacing.xs))
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            actions?.invoke(this)
        }
    }
}

private data class BadgeColors(
    val container: Color,
    val content: Color,
    val border: Color,
)

@Composable
private fun badgeColors(tone: RagnalaBadgeTone): BadgeColors = when (tone) {
    RagnalaBadgeTone.Neutral -> BadgeColors(
        container = MaterialTheme.colorScheme.surfaceVariant,
        content = MaterialTheme.colorScheme.onSurfaceVariant,
        border = MaterialTheme.colorScheme.outlineVariant,
    )
    RagnalaBadgeTone.Success -> BadgeColors(
        container = if (MaterialTheme.colorScheme.background == com.ragnala.pos.ui.theme.DarkBackground) DarkSuccessContainer else NaturalGreenContainer,
        content = MaterialTheme.colorScheme.onSurface,
        border = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
    )
    RagnalaBadgeTone.Warning -> BadgeColors(
        container = if (MaterialTheme.colorScheme.background == com.ragnala.pos.ui.theme.DarkBackground) DarkWarningContainer else WarmAmberContainer,
        content = MaterialTheme.colorScheme.onSurface,
        border = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
    )
    RagnalaBadgeTone.Error -> BadgeColors(
        container = if (MaterialTheme.colorScheme.background == com.ragnala.pos.ui.theme.DarkBackground) DarkErrorContainer else MutedRedContainer,
        content = MaterialTheme.colorScheme.onSurface,
        border = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
    )
}

@Composable
private fun moneyStyle(size: RagnalaMoneySize): TextStyle = when (size) {
    RagnalaMoneySize.Large -> RagnalaMoneyLarge
    RagnalaMoneySize.Medium -> RagnalaMoneyMedium
    RagnalaMoneySize.Small -> MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
}
