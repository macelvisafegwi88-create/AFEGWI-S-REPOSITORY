package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ChatType
import com.example.model.OnionHop
import com.example.ui.SessionGramViewModel
import com.example.ui.theme.*

@Composable
fun OnionCircuitDialog(
    viewModel: SessionGramViewModel,
    onDismiss: () -> Unit
) {
    val userAccount by viewModel.userAccount.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = SessionCyan)
                Text("Onion Routing Circuit", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "All messages, calls, and files are routed through 3 multi-hop decentralized service nodes. No single node knows both your IP address and destination.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                // Hops representation
                userAccount?.activeHops?.forEach { hop ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TelegramSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SessionCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SessionCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = hop.hopIndex.toString(),
                                    color = SessionCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${hop.role} • ${hop.location}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = hop.ipMask,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "${hop.latencyMs}ms",
                                color = SessionNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Dummy Traffic Obfuscation Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TelegramSurfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chaff Dummy Traffic", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Injects decoy packets to prevent ISP packet timing analysis", color = TextSecondary, fontSize = 10.sp)
                    }
                    Switch(
                        checked = userAccount?.dummyTrafficEnabled == true,
                        onCheckedChange = { viewModel.toggleDummyTraffic() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SessionCyan
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.rerollOnionCircuit() },
                colors = ButtonDefaults.buttonColors(containerColor = SessionCyan, contentColor = TelegramDarkBg)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Re-roll Circuit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = TelegramSurface
    )
}

@Composable
fun SecuritySettingsDialog(
    viewModel: SessionGramViewModel,
    onDismiss: () -> Unit
) {
    val userAccount by viewModel.userAccount.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showSeedPhrase by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = TelegramBlue)
                Text("Privacy & Cryptography", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(
                        text = "AFEGWICHAT accounts are purely cryptographic. No phone number or email is ever attached.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Session ID display
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Your Public Session ID (Hex)", color = SessionCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Surface(
                            onClick = { userAccount?.let { clipboardManager.setText(AnnotatedString(it.sessionId)) } },
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
                                    text = userAccount?.sessionId ?: "",
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TelegramBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // 12-Word Seed Phrase
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("12-Word Recovery Seed Phrase", color = SessionShieldGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            TextButton(
                                onClick = { showSeedPhrase = !showSeedPhrase },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (showSeedPhrase) "Hide" else "Reveal", color = TelegramBlue, fontSize = 12.sp)
                            }
                        }

                        if (showSeedPhrase) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TelegramSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    userAccount?.recoverySeed?.chunked(3)?.forEachIndexed { rowIndex, chunk ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            chunk.forEachIndexed { colIndex, word ->
                                                val num = (rowIndex * 3) + colIndex + 1
                                                Text(
                                                    text = "$num. $word",
                                                    color = TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Screen Security Toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TelegramSurfaceVariant)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Screen Security (Anti-Screenshot)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Prevents screenshot captures and blocks app preview in recents", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = userAccount?.screenSecurityEnabled == true,
                            onCheckedChange = { viewModel.toggleScreenSecurity() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TelegramBlue
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
            ) {
                Text("Done")
            }
        },
        containerColor = TelegramSurface
    )
}

@Composable
fun SafetyNumberDialog(
    safetyNumber: String = "4920 8371 9023 8812 7741 0928 3341 8290",
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SessionCyan)
                Text("Safety Number Verification", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Compare this cryptographic safety fingerprint with your contact. If they match, your communication is 100% immune to man-in-the-middle attacks.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                // Simulated QR Box
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR Code",
                        tint = Color.Black,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Numerical fingerprint
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TelegramSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = safetyNumber,
                        color = SessionCyan,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SessionCyan, contentColor = TelegramDarkBg)
            ) {
                Text("Mark as Verified", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = TelegramSurface
    )
}

@Composable
fun NewChatDialog(
    viewModel: SessionGramViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    var isSecretChat by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Start Encrypted Session", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the 66-character Session ID of your contact:", color = TextSecondary, fontSize = 12.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name / Nickname", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sessionId,
                    onValueChange = { sessionId = it },
                    label = { Text("Session ID (66 hex chars)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isSecretChat,
                        onCheckedChange = { isSecretChat = it },
                        colors = CheckboxDefaults.colors(checkedColor = SessionNeonGreen)
                    )
                    Text("Secret Chat (End-to-End Tunnel with Auto-Burn)", color = TextPrimary, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (name.isNotBlank()) name else "Peer " + (100..999).random()
                    val finalSessionId = if (sessionId.isNotBlank()) sessionId else "05" + System.currentTimeMillis() + "aa998877"
                    viewModel.createChat(
                        name = finalName,
                        sessionId = finalSessionId,
                        type = if (isSecretChat) ChatType.SECRET else ChatType.DIRECT
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
            ) {
                Text("Establish Handshake")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = TelegramSurface
    )
}

@Composable
fun NewGroupDialog(
    viewModel: SessionGramViewModel,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupTopic by remember { mutableStateOf("") }

    val availablePeers = listOf("Alex", "CipherZero", "Elena Rostova", "Neo", "Satoshi")
    val selectedPeers = remember { mutableStateListOf("CipherZero", "Elena Rostova") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = TelegramBlue)
                Text("New Encrypted Group", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Decentralized group chats route all communication through multi-hop onion nodes. Zero phone numbers or centralized server logs.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name", color = TextSecondary) },
                    placeholder = { Text("e.g. Cypherpunk Research") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = groupTopic,
                    onValueChange = { groupTopic = it },
                    label = { Text("Topic / Description", color = TextSecondary) },
                    placeholder = { Text("Encrypted decentralized discussion") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Add Initial Encrypted Members:", color = SessionCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availablePeers.forEach { peer ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedPeers.contains(peer),
                                onCheckedChange = { checked ->
                                    if (checked) selectedPeers.add(peer) else selectedPeers.remove(peer)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = TelegramBlue)
                            )
                            Text(peer, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (groupName.isNotBlank()) groupName else "Private Group #" + (100..999).random()
                    val finalTopic = if (groupTopic.isNotBlank()) groupTopic else "Decentralized Session Group"
                    viewModel.createGroup(finalName, finalTopic, selectedPeers.toList())
                },
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
            ) {
                Text("Create Group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = TelegramSurface
    )
}
