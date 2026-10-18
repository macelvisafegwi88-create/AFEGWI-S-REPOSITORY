package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.platform.LocalContext
import com.example.model.AttachmentType
import com.example.ui.SessionGramViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: SessionGramViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userAccount by viewModel.userAccount.collectAsState()

            // Dynamic Screen Security (FLAG_SECURE) based on Session privacy toggle
            LaunchedEffect(userAccount?.screenSecurityEnabled) {
                if (userAccount?.screenSecurityEnabled == true) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            SessionGramTheme {
                // Demand all required permissions on first launch after installation
                com.example.ui.components.FirstLaunchPermissionHandler()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TelegramDarkBg
                ) {
                    if (userAccount == null) {
                        AuthScreen(viewModel = viewModel)
                    } else {
                        SessionGramApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionGramApp(viewModel: SessionGramViewModel) {
    val activeChatId by viewModel.activeChatId.collectAsState()
    val activeStatusUser by viewModel.activeStatusUser.collectAsState()
    val isVaultOpen by viewModel.isVaultScreenOpen.collectAsState()
    val isCreateStatusOpen by viewModel.isCreateStatusOpen.collectAsState()
    val isOnionCircuitOpen by viewModel.isOnionCircuitDialogOpen.collectAsState()
    val isSecuritySettingsOpen by viewModel.isSecuritySettingsOpen.collectAsState()
    val isSafetyNumberOpen by viewModel.isSafetyNumberDialogOpen.collectAsState()
    val isNewChatOpen by viewModel.isNewChatDialogOpen.collectAsState()
    val isNewGroupOpen by viewModel.isNewGroupDialogOpen.collectAsState()
    val isSendLargeFileOpen by viewModel.isSendLargeFileDialogOpen.collectAsState()

    // Screen State Routing
    when {
        activeStatusUser != null -> {
            StatusViewerScreen(
                userStatus = activeStatusUser!!,
                viewModel = viewModel,
                onClose = { viewModel.closeStatusViewer() }
            )
        }
        isVaultOpen -> {
            LargeFileVaultScreen(
                viewModel = viewModel,
                onClose = { viewModel.setVaultScreenOpen(false) }
            )
        }
        activeChatId != null -> {
            ChatScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeChat() }
            )
        }
        else -> {
            HomeScreen(
                viewModel = viewModel,
                onOpenChat = { chatId -> viewModel.openChat(chatId) }
            )
        }
    }

    // WhatsApp Status Creation Dialog
    if (isCreateStatusOpen) {
        CreateStatusDialog(
            onDismiss = { viewModel.setCreateStatusOpen(false) },
            onPostText = { text, colors, duration ->
                viewModel.postTextStatus(text, colors, duration)
            },
            onPostPhoto = { photoType, caption, duration ->
                viewModel.postPhotoStatus(photoType, caption, duration)
            }
        )
    }

    // 3-Hop Onion Network Circuit Inspector Dialog
    if (isOnionCircuitOpen) {
        OnionCircuitDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setOnionCircuitDialogOpen(false) }
        )
    }

    // Session Cryptographic Keys & Privacy Dialog
    if (isSecuritySettingsOpen) {
        SecuritySettingsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setSecuritySettingsOpen(false) }
        )
    }

    // Safety Number Fingerprint & QR Code Verification Dialog
    if (isSafetyNumberOpen) {
        SafetyNumberDialog(
            onDismiss = { viewModel.setSafetyNumberDialogOpen(false) }
        )
    }

    // New Direct / Secret Session Chat Dialog
    if (isNewChatOpen) {
        NewChatDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setNewChatDialogOpen(false) }
        )
    }

    // New Decentralized Group Chat Dialog
    if (isNewGroupOpen) {
        NewGroupDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setNewGroupDialogOpen(false) }
        )
    }

    // Standalone Send Large File Dialog (Telegram file size liberty)
    if (isSendLargeFileOpen) {
        val context = LocalContext.current
        LargeFileDialog(
            onDismiss = { viewModel.setSendLargeFileDialogOpen(false) },
            onSend = { fileName, sizeBytes, formattedSize, type ->
                viewModel.sendLargeFile(fileName, sizeBytes, formattedSize, type)
                viewModel.setSendLargeFileDialogOpen(false)
            },
            onPickLocalFile = { uri ->
                viewModel.uploadFromLocalStorage(context, uri)
                viewModel.setSendLargeFileDialogOpen(false)
            }
        )
    }
}

@Composable
fun LargeFileDialog(
    onDismiss: () -> Unit,
    onSend: (String, Long, String, AttachmentType) -> Unit,
    onPickLocalFile: (Uri) -> Unit
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickLocalFile(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Send Large File (Telegram Liberty)",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Transfer massive raw files with zero compression and no artificial limits over Session's encrypted P2P onion relay:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Any File from Device Storage", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    "Or Choose Unlimited Raw Presets:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val filePresets = listOf(
                    Triple("Raw_4K_ProRes_Master.mov", 2362232012L to "2.20 GB", AttachmentType.VIDEO),
                    Triple("Full_Debian_Linux_DVD.iso", 4294967296L to "4.00 GB", AttachmentType.LARGE_FILE),
                    Triple("Quantum_Decentralized_Backup.tar", 1610612736L to "1.50 GB", AttachmentType.ARCHIVE),
                    Triple("Studio_Audio_Session_FLAC.zip", 838860800L to "800 MB", AttachmentType.AUDIO)
                )

                filePresets.forEach { (name, sizePair, type) ->
                    Surface(
                        onClick = { onSend(name, sizePair.first, sizePair.second, type) },
                        shape = RoundedCornerShape(8.dp),
                        color = TelegramSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = sizePair.second,
                                color = SessionNeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = TelegramSurface
    )
}

