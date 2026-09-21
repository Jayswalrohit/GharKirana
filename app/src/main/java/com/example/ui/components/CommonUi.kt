package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderStatus
import com.example.data.model.UserRole
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

fun formatNpr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 0
    return "Rs. ${formatter.format(amount)}"
}

@Composable
fun RoleSwitcherBar(
    currentRole: UserRole,
    onRoleSelect: (UserRole) -> Unit,
    unreadNotifCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(KiranaGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🇳🇵", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "GharKirana Express",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Janakpur Dham Kirana Network",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Switcher Pill Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoleChip(
                        title = "Customer",
                        emoji = "🛒",
                        isSelected = currentRole == UserRole.CUSTOMER,
                        onClick = { onRoleSelect(UserRole.CUSTOMER) },
                        testTag = "role_customer_btn"
                    )
                    RoleChip(
                        title = "Shop Admin",
                        emoji = "🏪",
                        isSelected = currentRole == UserRole.ADMIN,
                        onClick = { onRoleSelect(UserRole.ADMIN) },
                        testTag = "role_admin_btn"
                    )
                    RoleChip(
                        title = "Rider",
                        emoji = "🛵",
                        isSelected = currentRole == UserRole.RIDER,
                        onClick = { onRoleSelect(UserRole.RIDER) },
                        testTag = "role_rider_btn"
                    )
                }
            }
        }
    }
}

@Composable
fun RoleChip(
    title: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusBadge(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        OrderStatus.PLACED -> Pair(InfoBlueLight, InfoBlue)
        OrderStatus.CONFIRMED -> Pair(KiranaAmberLight, KiranaAmberDark)
        OrderStatus.PREPARING -> Pair(KiranaAmberLight, KiranaAmberDark)
        OrderStatus.READY_FOR_PICKUP -> Pair(KiranaGreenLight, KiranaGreenPrimary)
        OrderStatus.RIDER_ASSIGNED -> Pair(InfoBlueLight, InfoBlue)
        OrderStatus.PICKED_UP -> Pair(KiranaGreenLight, KiranaGreenDark)
        OrderStatus.OUT_FOR_DELIVERY -> Pair(SuccessGreenLight, SuccessGreen)
        OrderStatus.DELIVERED -> Pair(SuccessGreenLight, SuccessGreen)
        OrderStatus.CANCELLED -> Pair(ErrorRedLight, ErrorRed)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = status.title,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyPlaceholder(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
