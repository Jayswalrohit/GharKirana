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
fun GharKiranaBrandLogo(
    modifier: Modifier = Modifier,
    size: Int = 44,
    showTagline: Boolean = true,
    showNepaliSubtext: Boolean = showTagline
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = KiranaGreenPrimary,
            modifier = Modifier.size(size.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("🌾", fontSize = (size * 0.55).sp)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "GharKirana",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = KiranaGreenDark,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = KiranaAmberLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "🇳🇵 नेपाल",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = KiranaAmberDark,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            if (showNepaliSubtext) {
                Text(
                    text = "घर किराना • ताजा र शुद्ध",
                    style = MaterialTheme.typography.labelSmall,
                    color = KiranaGreenDark.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
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
