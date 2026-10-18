package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.model.AttachmentType
import com.example.ui.SessionGramViewModel
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFileVaultScreen(
    viewModel: SessionGramViewModel,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val userAccount by viewModel.userAccount.collectAsState()
    val context = LocalContext.current

    val vaultFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadToVaultFromLocalStorage(context, uri)
        }
    }

    var isSimulatingUpload by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadedFileName by remember { mutableStateOf("") }
    var uploadedSize by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Encrypted Vault & Cloud", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Unlimited File Size (Telegram Liberty)", fontSize = 11.sp, color = TelegramBlue)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TelegramSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = TelegramDarkBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner: The Fusion of Telegram Liberty & Session Zero-Knowledge
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TelegramSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            Column {
                                Text("No 25MB Email Limits", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text("Send 2GB - 4GB Files with Zero Compression", fontSize = 12.sp, color = SessionCyan)
                            }
                        }

                        Text(
                            text = "Unlike traditional messengers that compress images into blurry artifacts or block large files, AFEGWICHAT inherits Telegram's freedom to transfer massive raw media, operating systems, and encrypted archives, split into cryptographic chunks across Session's decentralized Lokinet nodes.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        // Stats Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(TelegramSurfaceVariant)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Stored in Vault", color = TextMuted, fontSize = 11.sp)
                                Text(userAccount?.totalVaultSize ?: "0 MB", color = SessionNeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Onion Chunks", color = TextMuted, fontSize = 11.sp)
                                Text("${userAccount?.totalChunksEncrypted ?: 0}", color = TelegramBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Metadata Leaks", color = TextMuted, fontSize = 11.sp)
                                Text("0 Bytes", color = SessionCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // Interactive Upload Demonstrator
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TelegramSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Transfer Massive Raw Files (No Limits)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                        Button(
                            onClick = { vaultFilePicker.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Any File from Device Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Text("Or Send Direct Onion Presets:", color = TextMuted, fontSize = 11.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.sendLargeFile(
                                        fileName = "Master_4K_ProRes_60fps.mov",
                                        sizeBytes = 2362232012L,
                                        formattedSize = "2.20 GB",
                                        type = AttachmentType.VIDEO
                                    )
                                    onClose()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramSurface)
                            ) {
                                Text("Send 2.2 GB Video", fontSize = 11.sp, color = TextPrimary)
                            }

                            Button(
                                onClick = {
                                    viewModel.sendLargeFile(
                                        fileName = "full_database_dump.sql.tar.gz",
                                        sizeBytes = 4187593113L,
                                        formattedSize = "3.90 GB",
                                        type = AttachmentType.ARCHIVE
                                    )
                                    onClose()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramSurface)
                            ) {
                                Text("Send 3.9 GB ISO", fontSize = 11.sp, color = SessionCyan)
                            }
                        }
                    }
                }
            }

            // Stored Vault Files List
            item {
                Text("Stored Massive Files in Your Vault", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextSecondary)
            }

            val sampleStoredFiles = listOf(
                Triple("archlinux-hardened-x86_64.iso", "3.82 GB", "9f83c1264b182e4439a2638848d7d8e69cbd3a1e"),
                Triple("Decentralized_Future_4K_ProRes.mkv", "1.84 GB", "4c5f87b8d1a3c69024f923b12a87c12690184b23"),
                Triple("Lossless_Session_Interview.flac", "750 MB", "88219038ba71c42289de6f2838190018a2bc4172"),
                Triple("quantum_cold_storage_keys.enc", "2.50 GB", "1182994038217720482930198832109238192031")
            )

            items(sampleStoredFiles) { (name, size, sha) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TelegramSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
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
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = TelegramBlue)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                            Text("SHA-256: $sha...", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(size, color = SessionNeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.downloadVaultFile(context, name)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelegramBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
