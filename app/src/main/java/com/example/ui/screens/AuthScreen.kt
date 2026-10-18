package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.SessionGramViewModel
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(viewModel: SessionGramViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isRestoring by viewModel.isRestoringCloudData.collectAsState()
    val cloudSyncError by viewModel.cloudSyncError.collectAsState()

    var isSigningInWithGoogle by remember { mutableStateOf(false) }
    var localAuthError by remember { mutableStateOf<String?>(null) }

    var selectedAuthTab by remember { mutableStateOf(0) } // 0 = Google Sign-In, 1 = Manual Restore
    var displayNameInput by remember { mutableStateOf("") }
    var restoreInput by remember { mutableStateOf("") }
    var restoreType by remember { mutableStateOf(0) } // 0 = Session ID, 1 = 12-Word Seed

    Scaffold(
        containerColor = TelegramDarkBg,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // App Brand Emblem
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(TelegramBlue.copy(alpha = 0.2f))
                    .border(2.dp, SessionCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "AFEGWICHAT Shield",
                    tint = SessionCyan,
                    modifier = Modifier.size(46.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AFEGWICHAT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Google Account Cloud Sync • Onion Encryption • Zero Logs",
                    fontSize = 12.sp,
                    color = SessionNeonGreen,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Syncing / Restoring Banner
            if (isRestoring || isSigningInWithGoogle) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TelegramSurface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SessionCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = SessionCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Restoring Cloud Data...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Validating User ID with Google. Reloading all encrypted messages, groups, statuses, and vault storage before starting real-time sync...",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            } else {
                // Auth Option Selector
                TabRow(
                    selectedTabIndex = selectedAuthTab,
                    containerColor = TelegramSurface,
                    contentColor = TelegramBlue,
                    divider = {},
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedAuthTab == 0,
                        onClick = { selectedAuthTab = 0 },
                        text = {
                            Text(
                                "Google Sign-In",
                                fontWeight = if (selectedAuthTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedAuthTab == 0) TelegramBlue else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedAuthTab == 1,
                        onClick = { selectedAuthTab = 1 },
                        text = {
                            Text(
                                "Manual Session",
                                fontWeight = if (selectedAuthTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedAuthTab == 1) TelegramBlue else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TelegramSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TelegramDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (selectedAuthTab == 0) {
                            // Primary Google Sign-In View
                            Text(
                                text = "Sign In with Device Gmail",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )

                            Text(
                                text = "Attach your AFEGWICHAT identity to your chosen Gmail account on this device. Once verified, all chats, group history, encrypted vault files, and statuses will be automatically restored and synchronized in real time.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            // Error notice if any
                            val displayError = localAuthError ?: cloudSyncError
                            if (displayError != null) {
                                Surface(
                                    color = Color(0xFFEF5350).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = displayError,
                                        color = Color(0xFFFF8A80),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            // Google Sign-In Button
                            Button(
                                onClick = {
                                    localAuthError = null
                                    isSigningInWithGoogle = true
                                    val credentialManager = CredentialManager.create(context)
                                    val clientId = try {
                                        context.getString(R.string.default_web_client_id)
                                    } catch (e: Exception) {
                                        localAuthError = "Google Client ID configuration missing"
                                        isSigningInWithGoogle = false
                                        return@Button
                                    }

                                    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                                    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

                                    coroutineScope.launch {
                                        try {
                                            val result = credentialManager.getCredential(context as Activity, request)
                                            val credential = result.credential
                                            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                                                val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                                                val user = authResult.user
                                                if (user != null) {
                                                    viewModel.restoreGoogleUser(
                                                        user = user,
                                                        onComplete = {
                                                            isSigningInWithGoogle = false
                                                        },
                                                        onError = { err ->
                                                            isSigningInWithGoogle = false
                                                            localAuthError = err
                                                        }
                                                    )
                                                } else {
                                                    isSigningInWithGoogle = false
                                                    localAuthError = "Google user was null"
                                                }
                                            } else {
                                                isSigningInWithGoogle = false
                                                localAuthError = "Unexpected credential format"
                                            }
                                        } catch (e: GetCredentialCancellationException) {
                                            Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
                                            isSigningInWithGoogle = false
                                        } catch (e: Exception) {
                                            Log.e("Auth", "Google Sign-In failed", e)
                                            isSigningInWithGoogle = false
                                            localAuthError = e.localizedMessage ?: "Sign-in failed. Please select your Google account."
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("google_signin_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Google Sign-In",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Continue with Google (Gmail)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = SessionCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Real-time Firestore cloud sync enabled",
                                    fontSize = 11.sp,
                                    color = SessionCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                        } else {
                            // Manual Session / Seed Phrase Restore
                            Text(
                                text = "Manual Cryptographic Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = restoreType == 0,
                                    onClick = { restoreType = 0 },
                                    label = { Text("Session ID", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TelegramBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = restoreType == 1,
                                    onClick = { restoreType = 1 },
                                    label = { Text("12-Word Seed", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TelegramBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = displayNameInput,
                                onValueChange = { displayNameInput = it },
                                label = { Text("Your Display Name") },
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
                                value = restoreInput,
                                onValueChange = { restoreInput = it },
                                label = { Text(if (restoreType == 0) "Session ID (66 hex chars)" else "12 Recovery Words") },
                                placeholder = { Text(if (restoreType == 0) "05..." else "word1 word2 word3...") },
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = TelegramBlue,
                                    unfocusedBorderColor = TelegramDivider
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val name = if (displayNameInput.isNotBlank()) displayNameInput else "Restored Operator"
                                    if (restoreType == 0) {
                                        viewModel.loginWithSessionId(restoreInput, name)
                                    } else {
                                        viewModel.restoreWithSeedPhrase(restoreInput, name)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SessionCyan, contentColor = TelegramDarkBg),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log In & Restore", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
