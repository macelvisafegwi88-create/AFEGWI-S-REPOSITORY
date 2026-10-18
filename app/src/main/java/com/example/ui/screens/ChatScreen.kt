package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.model.*
import com.example.ui.SessionGramViewModel
import com.example.ui.components.AudioWaveformPlayer
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: SessionGramViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val activeChat by viewModel.activeChat.collectAsState()
    val messages by viewModel.currentChatMessages.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val voiceDuration by viewModel.voiceRecordingDuration.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var inputText by remember { mutableStateOf("") }
    var showTimerMenu by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showGroupInfoSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadFromLocalStorage(context, uri)
            showAttachmentSheet = false
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadFromLocalStorage(context, uri)
            showAttachmentSheet = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val currentChat = activeChat ?: return
    val isGroupChat = currentChat.type == ChatType.GROUP

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isGroupChat) {
                                    showGroupInfoSheet = true
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(currentChat.avatarColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGroupChat) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Group",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = currentChat.name.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentChat.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (currentChat.isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Session",
                                        tint = SessionCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isGroupChat) "${currentChat.memberCount} members • Tap for group info" else currentChat.onlineStatus,
                                fontSize = 11.sp,
                                color = if (isGroupChat) TelegramBlue else SessionNeonGreen,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (isGroupChat) {
                        // Group info button
                        IconButton(onClick = { showGroupInfoSheet = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Group Info", tint = TelegramBlue)
                        }
                    }

                    // Disappearing Timer Selector Pill
                    Box {
                        IconButton(onClick = { showTimerMenu = true }) {
                            Badge(
                                containerColor = if (currentChat.disappearingTimer != DisappearingTimer.OFF) SessionNeonGreen else TextMuted
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = if (currentChat.disappearingTimer != DisappearingTimer.OFF) TelegramDarkBg else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTimerMenu,
                            onDismissRequest = { showTimerMenu = false },
                            modifier = Modifier.background(TelegramSurface)
                        ) {
                            Text(
                                text = "Disappearing Messages",
                                color = SessionCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            DisappearingTimer.values().forEach { timer ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(timer.label, color = TextPrimary)
                                            if (currentChat.disappearingTimer == timer) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = SessionCyan)
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.toggleDisappearingTimer(timer)
                                        showTimerMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Safety Numbers verification
                    IconButton(onClick = { viewModel.setSafetyNumberDialogOpen(true) }) {
                        Icon(Icons.Default.Shield, contentDescription = "Safety Number", tint = SessionCyan)
                    }

                    // Send Large File action
                    IconButton(onClick = { showAttachmentSheet = true }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attachments", tint = TelegramBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TelegramSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = TelegramDarkBg,
        bottomBar = {
            // Telegram Input Bar positioned well above the system navigation bar and keyboard
            Surface(
                color = TelegramSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isRecordingVoice) {
                        // Voice Recording Status Row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(TelegramSurfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(AccentRed)
                            )
                            Text(
                                text = "Recording Voice: ${voiceDuration}s",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.cancelVoiceRecording() }) {
                                Text("Cancel", color = AccentRed, fontSize = 12.sp)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.stopAndSendVoiceRecording() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SessionCyan)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send Voice", tint = TelegramDarkBg)
                        }
                    } else {
                        // Attachment Button (Unlimited files up to 4GB!)
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = TelegramBlue)
                        }

                        // Text Field
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    if (isGroupChat) "Message group via Onion..." else "Message via 3 Onion Hops...",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(TelegramSurfaceVariant),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = TelegramSurfaceVariant,
                                unfocusedContainerColor = TelegramSurfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            maxLines = 4
                        )

                        if (inputText.isBlank()) {
                            IconButton(
                                onClick = { viewModel.startVoiceRecording() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Record Voice", tint = Color.White)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
            ) {
                // Encryption notice banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TelegramSurfaceVariant.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, SessionCyan.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SessionCyan, modifier = Modifier.size(13.dp))
                                Text(
                                    text = if (isGroupChat) "Group Encrypted • ${currentChat.memberCount} Decentralized Onion Members" else "End-to-End Encrypted • Routed via 3 Lokinet Onion Hops",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isGroupChat = isGroupChat,
                        onDownloadClick = {
                            message.attachment?.let { att ->
                                viewModel.downloadAttachmentToLocalStorage(
                                    context = context,
                                    chatId = currentChat.id,
                                    messageId = message.id,
                                    attachment = att
                                )
                            } ?: viewModel.simulateDownloadProgress(currentChat.id, message.id)
                        },
                        onOpenFileClick = {
                            message.attachment?.let { att ->
                                val downloadsDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "AFEGWICHAT")
                                val localFile = File(downloadsDir, att.fileName)
                                if (localFile.exists()) {
                                    viewModel.openDownloadedFile(context, localFile)
                                } else {
                                    viewModel.downloadAttachmentToLocalStorage(
                                        context = context,
                                        chatId = currentChat.id,
                                        messageId = message.id,
                                        attachment = att
                                    ) { file ->
                                        if (file != null) viewModel.openDownloadedFile(context, file)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Group Information Bottom Sheet
    if (showGroupInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGroupInfoSheet = false },
            containerColor = TelegramSurface
        ) {
            var newMemberName by remember { mutableStateOf("") }
            var newMemberSessionId by remember { mutableStateOf("") }
            var showAddMemberForm by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(currentChat.avatarColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text(currentChat.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Text("${currentChat.memberCount} Decentralized Members", fontSize = 13.sp, color = SessionNeonGreen)
                    }
                }

                if (currentChat.topic.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TelegramSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Topic", color = TelegramBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(currentChat.topic, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Group Members", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    TextButton(onClick = { showAddMemberForm = !showAddMemberForm }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Member", color = WhatsAppGreen, fontSize = 12.sp)
                    }
                }

                if (showAddMemberForm) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TelegramSurfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newMemberName,
                            onValueChange = { newMemberName = it },
                            label = { Text("Member Name / Pseudonym") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                        OutlinedTextField(
                            value = newMemberSessionId,
                            onValueChange = { newMemberSessionId = it },
                            label = { Text("Session ID (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                        Button(
                            onClick = {
                                if (newMemberName.isNotBlank()) {
                                    viewModel.addGroupMember(currentChat.id, newMemberName, newMemberSessionId)
                                    newMemberName = ""
                                    newMemberSessionId = ""
                                    showAddMemberForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Confirm & Handshake")
                        }
                    }
                }

                // Member list
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentChat.members) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TelegramSurfaceVariant.copy(alpha = 0.6f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(member.avatarColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(member.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Column {
                                    Text(member.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(member.sessionId.take(12) + "...", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (member.role == "Admin") TelegramBlue.copy(alpha = 0.2f) else TelegramDarkBg
                            ) {
                                Text(
                                    text = member.role,
                                    color = if (member.role == "Admin") TelegramBlue else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Attachment Modal Sheet (Highlighting Telegram's File Size Liberty)
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = TelegramSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = TelegramBlue)
                    Column {
                        Text(
                            text = "Unlimited File Sharing (Telegram Liberty)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Send up to 4 GB per file with zero metadata logging",
                            fontSize = 12.sp,
                            color = SessionCyan
                        )
                    }
                }

                // Device Local Storage Upload Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp),
                        color = TelegramBlue.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(28.dp))
                            Text("Device File", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Pick any document, ISO, archive", color = TextSecondary, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }

                    Surface(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = SessionCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SessionCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = SessionCyan, modifier = Modifier.size(28.dp))
                            Text("Gallery Media", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Raw photos & uncompressed video", color = TextSecondary, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }

                Text(
                    text = "Or choose unlimited raw test presets:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val presets = listOf(
                    Triple("4K Uncompressed Raw Master (.mkv)", 1975684915L to "1.84 GB", AttachmentType.VIDEO),
                    Triple("Ubuntu 24.04 LTS OS Image (.iso)", 4101881856L to "3.82 GB", AttachmentType.LARGE_FILE),
                    Triple("Lossless Studio Album (FLAC Master .zip)", 786432000L to "750 MB", AttachmentType.ARCHIVE),
                    Triple("Cryptographic Cold Storage Backup (.enc)", 2684354560L to "2.50 GB", AttachmentType.DOCUMENT)
                )

                presets.forEach { (title, sizePair, type) ->
                    Surface(
                        onClick = {
                            viewModel.sendLargeFile(
                                fileName = title.substringBefore(" ("),
                                sizeBytes = sizePair.first,
                                formattedSize = sizePair.second,
                                type = type
                            )
                            showAttachmentSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = TelegramSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(TelegramBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (type) {
                                            AttachmentType.VIDEO -> Icons.Default.Videocam
                                            AttachmentType.ARCHIVE -> Icons.Default.FolderZip
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = TelegramBlue
                                    )
                                }
                                Column {
                                    Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Zero compression • P2P Mesh Streaming", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = sizePair.second,
                                color = SessionNeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isGroupChat: Boolean = false,
    onDownloadClick: () -> Unit,
    onOpenFileClick: () -> Unit = {}
) {
    val isOut = message.isOutgoing
    val bubbleColor = if (isOut) TelegramBubbleOut else TelegramBubbleIn
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 100.dp, max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOut) 16.dp else 4.dp,
                        bottomEnd = if (isOut) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Group Chat Sender Name Header (Telegram style)
            if (isGroupChat && !isOut && message.senderName != "You") {
                Text(
                    text = message.senderName,
                    color = Color(message.senderColorHex),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Reply Preview if present
            if (message.replyTo != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TelegramDarkBg.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(message.replyTo.senderName, color = TelegramBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(message.replyTo.snippet, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            // Disappearing Timer Countdown indicator
            if (message.disappearingTimer != DisappearingTimer.OFF) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentRed.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Burn Timer",
                        tint = AccentRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Vaporizes in ${message.disappearingTimer.label}",
                        color = AccentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Text content
            if (message.text.isNotBlank() && !message.isVoice) {
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }

            // Large File Attachment Presentation
            if (message.attachment != null) {
                val att = message.attachment
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TelegramDarkBg.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, TelegramBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (att.type) {
                                        AttachmentType.VIDEO -> Icons.Default.PlayCircle
                                        AttachmentType.ARCHIVE -> Icons.Default.FolderZip
                                        else -> Icons.Default.FileDownload
                                    },
                                    contentDescription = "File",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = att.fileName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${att.formattedSize} • P2P Stream Ready",
                                    color = SessionNeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = "SHA-256: ${att.sha256Checksum.take(16)}...",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        if (att.transferProgress < 1.0f) {
                            LinearProgressIndicator(
                                progress = { att.transferProgress },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = TelegramBlue,
                                trackColor = TelegramSurfaceVariant
                            )
                            Text(
                                text = "Transferring to device: ${(att.transferProgress * 100).toInt()}% • ${att.transferSpeed}",
                                color = SessionCyan,
                                fontSize = 10.sp
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SessionNeonGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Ready in Local Storage",
                                            fontSize = 11.sp,
                                            color = SessionNeonGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = onDownloadClick,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TelegramBlue,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save to Device", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Saved to Downloads/AFEGWICHAT",
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                    TextButton(
                                        onClick = onOpenFileClick,
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Open File", fontSize = 11.sp, color = SessionCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Voice message representation
            if (message.isVoice) {
                AudioWaveformPlayer(
                    durationSec = message.voiceDurationSec,
                    waveforms = message.voiceWaveform,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Timestamp and check marks
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    fontSize = 10.sp,
                    color = TextMuted
                )
                if (isOut) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = if (message.status == MessageStatus.READ) SessionCyan else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
