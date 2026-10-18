package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

fun getRequiredAppPermissions(): Array<String> {
    val permissions = mutableListOf<String>()
    permissions.add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return permissions.toTypedArray()
}

fun checkUngrantedPermissions(context: Context): List<String> {
    return getRequiredAppPermissions().filter { perm ->
        ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun FirstLaunchPermissionHandler() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("afegwichat_prefs", Context.MODE_PRIVATE) }
    var showDialog by remember { mutableStateOf(false) }

    val requiredPerms = remember { getRequiredAppPermissions() }
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (!allGranted) {
            val ungranted = checkUngrantedPermissions(context)
            if (ungranted.isNotEmpty()) {
                showDialog = true
            }
        } else {
            showDialog = false
        }
    }

    LaunchedEffect(Unit) {
        val hasDemanded = prefs.getBoolean("has_demanded_permissions_v2", false)
        val ungranted = checkUngrantedPermissions(context)

        // Demand permissions on first launch immediately
        if (!hasDemanded || ungranted.isNotEmpty()) {
            prefs.edit().putBoolean("has_demanded_permissions_v2", true).apply()
            permissionsLauncher.launch(requiredPerms)
        }
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = TelegramSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SessionCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SessionCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SessionCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AFEGWICHAT Permissions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Setup required for smooth device operations",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "To enable end-to-end encrypted voice notes, real-time message sync, and seamless unlimited file uploads and downloads between the app and device storage, please grant the following permissions:",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    PermissionItemRow(
                        icon = Icons.Default.Mic,
                        title = "Microphone",
                        desc = "Record zero-knowledge encrypted voice messages"
                    )

                    PermissionItemRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        desc = "Alerts for incoming onion messages and download completions"
                    )

                    PermissionItemRow(
                        icon = Icons.Default.Folder,
                        title = "Local Storage & Media",
                        desc = "Smooth download and upload of raw media and vault files"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { showDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                showDialog = false
                                permissionsLauncher.launch(requiredPerms)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SessionCyan,
                                contentColor = TelegramDarkBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text(
                                "Grant Permissions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TelegramSurfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TelegramBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(desc, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
