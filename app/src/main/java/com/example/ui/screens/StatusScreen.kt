package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.SessionGramViewModel
import com.example.ui.components.StatusRingAvatar
import com.example.ui.theme.*

@Composable
fun StatusTabContent(
    viewModel: SessionGramViewModel,
    statuses: List<UserStatus>
) {
    val myStatus = statuses.find { it.isMyStatus }
    val recentStatuses = statuses.filter { !it.isMyStatus && it.hasUnseen }
    val viewedStatuses = statuses.filter { !it.isMyStatus && !it.hasUnseen }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // My Status Card (WhatsApp-style)
            item {
                Surface(
                    onClick = {
                        if (myStatus != null && myStatus.items.isNotEmpty()) {
                            viewModel.openStatusViewer(myStatus)
                        } else {
                            viewModel.setCreateStatusOpen(true)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = TelegramSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(TelegramSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "My Status",
                                    tint = TelegramBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            // Plus badge
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(TelegramDarkBg)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "My Status",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                myStatus?.items?.firstOrNull()?.duration?.let { dur ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = WhatsAppGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = dur.label,
                                            color = WhatsAppGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (myStatus?.items?.isNotEmpty() == true) {
                                    "${myStatus.items.first().timestamp} • Tap to view"
                                } else {
                                    "Tap to add status (24h, 7d, or 30d)"
                                },
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        if (myStatus?.items?.isNotEmpty() == true) {
                            IconButton(onClick = { viewModel.setCreateStatusOpen(true) }) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add More",
                                    tint = WhatsAppGreen
                                )
                            }
                        }
                    }
                }
            }

            // Duration info note banner
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TelegramSurfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, WhatsAppGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Status display times can be configured to 24 Hours, 7 Days, or 30 Days.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Recent Updates Section
            if (recentStatuses.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent updates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreen
                    )
                }

                items(recentStatuses, key = { it.id }) { userStatus ->
                    StatusListItem(
                        userStatus = userStatus,
                        onClick = { viewModel.openStatusViewer(userStatus) }
                    )
                }
            }

            // Viewed Updates Section
            if (viewedStatuses.isNotEmpty()) {
                item {
                    Text(
                        text = "Viewed updates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }

                items(viewedStatuses, key = { it.id }) { userStatus ->
                    StatusListItem(
                        userStatus = userStatus,
                        onClick = { viewModel.openStatusViewer(userStatus) }
                    )
                }
            }

            // WhatsApp Privacy Footer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Your status updates are end-to-end encrypted",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // WhatsApp-style Floating Action Buttons (Camera + Pencil)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Text Status Pencil FAB
            SmallFloatingActionButton(
                onClick = { viewModel.setCreateStatusOpen(true) },
                containerColor = TelegramSurfaceVariant,
                contentColor = WhatsAppGreen,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Text Status", modifier = Modifier.size(20.dp))
            }

            // Camera / Media Status FAB
            FloatingActionButton(
                onClick = { viewModel.setCreateStatusOpen(true) },
                containerColor = WhatsAppGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera Status")
            }
        }
    }
}

@Composable
fun StatusListItem(
    userStatus: UserStatus,
    onClick: () -> Unit
) {
    val firstItem = userStatus.items.firstOrNull()
    val duration = firstItem?.duration ?: StatusDuration.HOURS_24

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = TelegramSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusRingAvatar(
                name = userStatus.userName,
                avatarColorHex = userStatus.avatarColorHex,
                size = 52.dp,
                hasUnseenStatus = userStatus.hasUnseen,
                statusCount = userStatus.items.size
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userStatus.userName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = firstItem?.timestamp ?: "Recently",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    // Status Duration Badge (24h, 7d, 30d)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (duration == StatusDuration.HOURS_24) TextMuted.copy(alpha = 0.2f) else SessionCyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = duration.label,
                            color = if (duration == StatusDuration.HOURS_24) TextSecondary else SessionCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStatusDialog(
    onDismiss: () -> Unit,
    onPostText: (String, List<Long>, StatusDuration) -> Unit,
    onPostPhoto: (String, String, StatusDuration) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Text, 1 = Photo
    var statusText by remember { mutableStateOf("") }
    var photoCaption by remember { mutableStateOf("") }
    var selectedPhotoType by remember { mutableStateOf("cyber_setup") }
    var selectedDuration by remember { mutableStateOf(StatusDuration.DAYS_7) } // Defaults to 7 days, selectable between 24h, 7d, 30d!

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onPostPhoto("device_photo", photoCaption, selectedDuration)
        }
    }

    val gradientPresets = listOf(
        listOf(0xFF833AB4, 0xFFFD1D1D, 0xFFFCB045), // Instagram/sunset
        listOf(0xFF134E5E, 0xFF71B280),             // Emerald cyber
        listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364), // Dark deep slate
        listOf(0xFF8A2387, 0xFFE94057, 0xFFF27121), // Fire ember
        listOf(0xFF1F4037, 0xFF99F2C8)              // Mint neon
    )
    var currentGradientIndex by remember { mutableStateOf(0) }
    val activeGradient = gradientPresets[currentGradientIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(TelegramDarkBg)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }

                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = WhatsAppGreen,
                            divider = {},
                            modifier = Modifier.width(200.dp)
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Text Status", fontSize = 13.sp) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Photo Status", fontSize = 13.sp) }
                            )
                        }

                        if (selectedTab == 0) {
                            IconButton(onClick = {
                                currentGradientIndex = (currentGradientIndex + 1) % gradientPresets.size
                            }) {
                                Icon(Icons.Default.Palette, contentDescription = "Change Color", tint = WhatsAppGreen)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }

                    // Display Time Selector (24 Hours, 7 Days, 30 Days)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Display Duration:",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusDuration.values().forEach { duration ->
                            val isSelected = selectedDuration == duration
                            Surface(
                                onClick = { selectedDuration = duration },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) WhatsAppGreen else TelegramSurfaceVariant,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                    Text(
                                        text = duration.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = TelegramDarkBg,
            bottomBar = {
                Surface(
                    color = TelegramDarkBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SessionCyan, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Contacts Only • Disappears in ${selectedDuration.label}",
                                color = SessionCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        FloatingActionButton(
                            onClick = {
                                if (selectedTab == 0 && statusText.isNotBlank()) {
                                    onPostText(statusText, activeGradient, selectedDuration)
                                } else if (selectedTab == 1) {
                                    onPostPhoto(selectedPhotoType, photoCaption, selectedDuration)
                                }
                            },
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Post Status")
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (selectedTab == 0) {
                    // WhatsApp Text Status Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(activeGradient.map { Color(it) }))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextField(
                            value = statusText,
                            onValueChange = { statusText = it },
                            placeholder = {
                                Text(
                                    "Type a status...",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Photo Status Creation
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Photo from Device Storage", fontWeight = FontWeight.Bold)
                        }

                        Text("Or Choose Visual Status Preset:", color = TextMuted, fontSize = 12.sp)

                        val photoOptions = listOf(
                            "cyber_setup" to "Cybersecurity Command Center",
                            "city_night" to "Anonymous Node Skyline",
                            "code_terminal" to "Encrypted Kernel Terminal"
                        )

                        photoOptions.forEach { (type, label) ->
                            Surface(
                                onClick = { selectedPhotoType = type },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedPhotoType == type) TelegramSurfaceVariant else TelegramSurface,
                                border = if (selectedPhotoType == type) androidx.compose.foundation.BorderStroke(1.5.dp, WhatsAppGreen) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = WhatsAppGreen)
                                    Text(label, color = TextPrimary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = photoCaption,
                            onValueChange = { photoCaption = it },
                            label = { Text("Add a caption...", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = WhatsAppGreen,
                                unfocusedBorderColor = TelegramDivider
                            )
                        )
                    }
                }
            }
        }
    }
}
