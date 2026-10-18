package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.SessionGramViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewerScreen(
    userStatus: UserStatus,
    viewModel: SessionGramViewModel,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var currentItemIndex by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var showViewersSheet by remember { mutableStateOf(false) }

    val items = userStatus.items
    val currentItem = items.getOrNull(currentItemIndex) ?: run {
        onClose()
        return
    }

    val progressAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Auto-advance timer (5 seconds per story)
    LaunchedEffect(currentItemIndex, isPaused) {
        if (!isPaused) {
            progressAnim.snapTo(0f)
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            if (currentItemIndex < items.size - 1) {
                currentItemIndex += 1
            } else {
                onClose()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.35f) {
                            // Tap left: previous item
                            if (currentItemIndex > 0) {
                                currentItemIndex -= 1
                            }
                        } else {
                            // Tap right: next item
                            if (currentItemIndex < items.size - 1) {
                                currentItemIndex += 1
                            } else {
                                onClose()
                            }
                        }
                    }
                )
            }
    ) {
        // Status Content (Text Gradient or Photo)
        if (currentItem.type == StatusType.TEXT_GRADIENT) {
            val colors = if (currentItem.gradientColors.isNotEmpty()) {
                currentItem.gradientColors.map { Color(it) }
            } else {
                listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors))
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentItem.text,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )
            }
        } else {
            // Photo status view
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TelegramDarkBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(TelegramSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (currentItem.photoPlaceholderType) {
                                "cyber_setup" -> Icons.Default.Computer
                                "code_terminal" -> Icons.Default.Terminal
                                else -> Icons.Default.Photo
                            },
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(72.dp)
                        )
                    }

                    if (currentItem.caption.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TelegramSurface.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = currentItem.caption,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Overlay: Progress Bars & Contact Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 12.dp, end = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            // Multi-segment story progress indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentItemIndex -> 1f
                        index == currentItemIndex -> progressAnim.value
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            // Contact info and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(userStatus.avatarColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userStatus.userName.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = userStatus.userName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SessionCyan.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = currentItem.duration.label,
                                    color = SessionCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${currentItem.timestamp} • Encrypted Session Status",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Overlay (Reply or Viewers analytics)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            if (userStatus.isMyStatus) {
                // "My Status" view count and viewers button
                Surface(
                    onClick = { showViewersSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Viewers",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${currentItem.viewers.size} viewers",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // Reply to contact's status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply to ${userStatus.userName}...", color = Color.White.copy(alpha = 0.6f)) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.6f)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                // Find or open chat to reply
                                val targetChat = viewModel.chats.value.find { it.name == userStatus.userName }
                                if (targetChat != null) {
                                    viewModel.openChat(targetChat.id)
                                    viewModel.sendMessage("Replying to status: \"${currentItem.text.take(30)}\" — $replyText")
                                }
                                onClose()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Reply", tint = Color.White)
                    }
                }
            }
        }
    }

    // Viewers Bottom Sheet for "My Status"
    if (showViewersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showViewersSheet = false },
            containerColor = TelegramSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = WhatsAppGreen)
                    Text(
                        text = "Viewed by ${currentItem.viewers.size}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (currentItem.viewers.isEmpty()) {
                    Text("No contacts have decrypted this status yet.", color = TextSecondary, fontSize = 14.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(currentItem.viewers) { viewer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TelegramSurfaceVariant)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(TelegramBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = viewer.name.take(2).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column {
                                        Text(viewer.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(viewer.sessionId.take(12) + "...", color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                                Text(viewer.viewedAt, color = WhatsAppGreen, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
