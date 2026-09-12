package com.codigitech.ft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.AccountWithBalance
import com.codigitech.ft.ui.theme.AppIcons
import com.codigitech.ft.ui.theme.tabular

val AccountType.icon: ImageVector
    get() = when (this) {
        AccountType.CASH -> AppIcons.Wallet
        AccountType.BANK -> AppIcons.Bank
        AccountType.CARD -> AppIcons.CreditCard
        AccountType.OTHER -> AppIcons.Layers
    }

@Composable
fun AccountType.tint(): Color = when (this) {
    AccountType.CASH -> MaterialTheme.colorScheme.primary
    AccountType.BANK -> MaterialTheme.colorScheme.tertiary
    AccountType.CARD -> MaterialTheme.colorScheme.error
    AccountType.OTHER -> MaterialTheme.colorScheme.secondary
}

@Composable
fun AccountTypeIcon(type: AccountType, size: Int = 40) {
    val tint = type.tint()
    Box(Modifier.size(size.dp).background(tint.copy(alpha = 0.14f), MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
        Icon(type.icon, contentDescription = type.label, tint = tint, modifier = Modifier.size((size * 0.55f).dp))
    }
}

/** Compact card for the horizontal carousel on Home. */
@Composable
fun AccountCarouselCard(item: AccountWithBalance, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier.width(168.dp), containerColor = MaterialTheme.colorScheme.surfaceContainerLow, onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            AccountTypeIcon(item.account.type, size = 36)
            VSpace(14)
            Text(item.account.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.account.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VSpace(6)
            MoneyText(item.balance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Full-width row for the Accounts tab. */
@Composable
fun AccountListRow(
    item: AccountWithBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val archived = item.account.isArchived
    SurfaceCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AccountTypeIcon(item.account.type)
            Column(Modifier.weight(1f)) {
                Text(
                    item.account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (archived) "Archived · ${item.account.type.label}" else item.account.type.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(item.balance, style = MaterialTheme.typography.titleMedium.tabular, fontWeight = FontWeight.SemiBold, colorForSign = !archived)
            if (trailing != null) { Spacer(Modifier.width(4.dp)); trailing() }
        }
    }
}
