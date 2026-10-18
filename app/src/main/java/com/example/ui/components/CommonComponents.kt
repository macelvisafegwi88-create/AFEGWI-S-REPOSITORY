package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StatusRingAvatar(
    name: String,
    avatarColorHex: Long,
    size: Dp = 52.dp,
    hasUnseenStatus: Boolean = false,
    statusCount: Int = 1,
    isOnline: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val ringBrush = if (hasUnseenStatus) {
        Brush.sweepGradient(
            colors = listOf(
                StatusRingStart.copy(alpha = pulseAlpha),
                StatusRingEnd.copy(alpha = pulseAlpha),
                TelegramBlue,
                StatusRingStart.copy(alpha = pulseAlpha)
            )
        )
    } else {
        Brush.linearGradient(listOf(TextMuted.copy(alpha = 0.5f), TextMuted.copy(alpha = 0.3f)))
    }

    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // WhatsApp Status Gradient Ring if applicable
        if (hasUnseenStatus) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.5.dp, ringBrush, CircleShape)
            )
        }

        // Inner Avatar Circle
        Box(
            modifier = Modifier
                .size(if (hasUnseenStatus) size - 6.dp else size)
                .clip(CircleShape)
                .background(Color(avatarColorHex)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp
            )
        }

        // Online/Session indicator badge
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(TelegramDarkBg)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(SessionNeonGreen)
            )
        }
    }
}

@Composable
fun OnionHopBadge(
    hopsCount: Int = 3,
    latencyMs: Int = 34,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = TelegramSurfaceVariant.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SessionCyan.copy(alpha = 0.4f)),
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SessionCyan)
            )
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Onion Shield",
                tint = SessionCyan,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${hopsCount} Hops • ${latencyMs}ms",
                color = SessionCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun HexSessionIdBadge(
    sessionId: String,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit
) {
    val shortId = if (sessionId.length > 16) {
        sessionId.take(8) + "..." + sessionId.takeLast(6)
    } else {
        sessionId
    }

    Surface(
        onClick = onCopy,
        shape = RoundedCornerShape(8.dp),
        color = TelegramDarkBg.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, TelegramBlue.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = "Session ID",
                tint = TelegramBlue,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = shortId,
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = TextMuted,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
fun AudioWaveformPlayer(
    durationSec: Int,
    waveforms: List<Float>,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TelegramSurfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(TelegramBlue)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val bars = if (waveforms.isEmpty()) listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.3f, 0.8f, 0.6f, 0.4f) else waveforms
            bars.forEachIndexed { index, weight ->
                val barHeight = (weight * 20).coerceIn(4f, 22f).dp
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < bars.size / 2 && isPlaying) TelegramBlue else TextSecondary.copy(alpha = 0.7f))
                )
            }
        }

        Text(
            text = "0:${durationSec.toString().padStart(2, '0')}",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
