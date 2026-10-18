package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.model.*
import com.example.ui.SessionGramViewModel
import com.example.ui.components.HexSessionIdBadge
import com.example.ui.components.OnionHopBadge
import com.example.ui.components.StatusRingAvatar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SessionGramViewModel,
    onOpenChat: (String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val userAccount by viewModel.userAccount.collectAsState()
    val filteredChats by viewModel.filteredChats.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val statuses by viewModel.statuses.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val currentUser = userAccount ?: return

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = TelegramSurface,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(310.dp)
            ) {
                // Drawer Header with Session Identity & Onion Status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TelegramDarkBg)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Session",
                                    tint = SessionCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = currentUser.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = TextPrimary
                                )
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.let { email ->
                                    Text(
                                        text = email,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = SessionCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cloud Real-Time Sync Active",
                                        fontSize = 11.sp,
                                        color = SessionCyan,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Short Session ID Badge
                        HexSessionIdBadge(
                            sessionId = currentUser.sessionId,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            clipboardManager.setText(AnnotatedString(currentUser.sessionId))
                        }
                    }
                }

                HorizontalDivider(color = TelegramDivider, thickness = 1.dp)

                // Navigation Items
                LazyColumn(modifier = Modifier.padding(vertical = 8.dp)) {
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = TelegramBlue) },
                            label = { Text("New Encrypted Group") },
                            badge = { Text("Multi-peer", fontSize = 11.sp, color = TelegramBlue) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setNewGroupDialogOpen(true)
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.CloudQueue, contentDescription = null, tint = TelegramBlue) },
                            label = { Text("Saved Vault (Up to 4GB Files)") },
                            badge = { Text(currentUser.totalVaultSize, fontSize = 11.sp, color = TextSecondary) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setVaultScreenOpen(true)
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = WhatsAppGreen) },
                            label = { Text("WhatsApp Status Updates") },
                            badge = { Text("24h/7d/30d", fontSize = 11.sp, color = WhatsAppGreen) },
                            selected = selectedTab == 2,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setSelectedTab(2)
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Hub, contentDescription = null, tint = SessionCyan) },
                            label = { Text("Onion Routing Circuit") },
                            badge = { Text("3 Hops", fontSize = 11.sp, color = SessionCyan) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setOnionCircuitDialogOpen(true)
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = SessionPurple) },
                            label = { Text("Privacy & Session Keys") },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setSecuritySettingsOpen(true)
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.UploadFile, contentDescription = null, tint = SessionShieldGold) },
                            label = { Text("Send Large File (No Limit)") },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setSendLargeFileDialogOpen(true)
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                    item {
                        HorizontalDivider(
                            color = TelegramDivider,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = TextSecondary) },
                            label = { Text("Safety Verification Codes") },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setSafetyNumberDialogOpen(true)
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = AccentRed) },
                            label = { Text("Log Out / Switch Account", color = AccentRed, fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showLogoutDialog = true
                            },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(TelegramSurface)) {
                    TopAppBar(
                        title = {
                            if (isSearchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Search chats, session keys, files...", color = TextMuted) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "AFEGWICHAT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = TextPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Private",
                                        tint = SessionCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (isSearchActive) {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Close search", tint = TextPrimary)
                                }
                            } else {
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Navigation drawer", tint = TextPrimary)
                                }
                            }
                        },
                        actions = {
                            if (!isSearchActive) {
                                // 3-hop Onion Status Pill
                                OnionHopBadge(hopsCount = 3, latencyMs = 34) {
                                    viewModel.setOnionCircuitDialogOpen(true)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = TelegramSurface,
                            titleContentColor = TextPrimary
                        )
                    )

                    // Telegram Folders / Category Tabs
                    val tabs = listOf("All Chats", "Direct", "Status", "Channels", "Groups", "Vault")
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = TelegramSurface,
                        contentColor = TelegramBlue,
                        edgePadding = 12.dp,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { viewModel.setSelectedTab(index) },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == index) TelegramBlue else TextSecondary,
                                            fontSize = 14.sp
                                        )
                                        if (index == 2) {
                                            // WhatsApp Status badge dot
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(WhatsAppGreen)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isFabExpanded) {
                        // WhatsApp Status creation option
                        FloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                viewModel.setCreateStatusOpen(true)
                            },
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "New Status", modifier = Modifier.size(20.dp))
                        }

                        // Unlimited Large File sharing option
                        FloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                viewModel.setSendLargeFileDialogOpen(true)
                            },
                            containerColor = TelegramBlue,
                            contentColor = Color.White,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Send File", modifier = Modifier.size(20.dp))
                        }

                        // New Decentralized Group option
                        FloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                viewModel.setNewGroupDialogOpen(true)
                            },
                            containerColor = SessionPurple,
                            contentColor = Color.White,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "New Group", modifier = Modifier.size(20.dp))
                        }

                        // Direct Session Chat option
                        FloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                viewModel.setNewChatDialogOpen(true)
                            },
                            containerColor = SessionCyan,
                            contentColor = TelegramDarkBg,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "New Chat", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Main FAB
                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = TelegramBlue,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = "New Message / Action"
                        )
                    }
                }
            },
            containerColor = TelegramDarkBg
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (selectedTab == 2) {
                    // Dedicated Status Tab Screen (WhatsApp Status experience)
                    StatusTabContent(
                        viewModel = viewModel,
                        statuses = statuses
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // WhatsApp Status Stories Row (Always accessible at top of chat list)
                        item {
                            StatusStoriesRow(
                                statuses = statuses,
                                onMyStatusClick = { viewModel.setCreateStatusOpen(true) },
                                onContactStatusClick = { userStatus ->
                                    viewModel.openStatusViewer(userStatus)
                                }
                            )
                        }

                        item {
                            HorizontalDivider(
                                color = TelegramDivider,
                                thickness = 6.dp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Chat List
                        if (filteredChats.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = TelegramSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(TelegramBlue.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChatBubbleOutline,
                                                contentDescription = null,
                                                tint = TelegramBlue,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Text(
                                            text = "No chats yet in this folder",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = TextPrimary
                                        )

                                        Text(
                                            text = "Your account is completely private with no phone number attached. Share your Session ID with peers to start chatting with 3-hop onion encryption, or create an encrypted group.",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = 17.sp
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(currentUser.sessionId))
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = TelegramSurfaceVariant)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Copy My ID", fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = { viewModel.setNewChatDialogOpen(true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                                            ) {
                                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("New Chat", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredChats, key = { it.id }) { chat ->
                                ChatListItem(
                                    chat = chat,
                                    onClick = { onOpenChat(chat.id) }
                                )
                                HorizontalDivider(
                                    color = TelegramDivider,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 76.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = AccentRed)
                    Text("Log Out of Session?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "Logging out removes your active session from this device. You can restore your identity and chats at any time using your 12-word seed phrase or Session ID.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = TelegramSurface
        )
    }
}

@Composable
fun StatusStoriesRow(
    statuses: List<UserStatus>,
    onMyStatusClick: () -> Unit,
    onContactStatusClick: (UserStatus) -> Unit
) {
    val myStatus = statuses.find { it.isMyStatus }
    val contactStatuses = statuses.filter { !it.isMyStatus }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TelegramSurface)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DonutLarge,
                    contentDescription = "Status",
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Encrypted Status (WhatsApp)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WhatsAppGreen
                )
            }
            Text(
                text = "24-Hour Ephemeral",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // "My Status" circle with + badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(66.dp)
                    .clickable { onMyStatusClick() }
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
                    Box(
                        modifier = Modifier
                            .size(18.dp)
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
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "My Status",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Contact statuses
            contactStatuses.forEach { contactStatus ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(66.dp)
                        .clickable { onContactStatusClick(contactStatus) }
                ) {
                    StatusRingAvatar(
                        name = contactStatus.userName,
                        avatarColorHex = contactStatus.avatarColorHex,
                        size = 54.dp,
                        hasUnseenStatus = contactStatus.hasUnseen,
                        statusCount = contactStatus.items.size
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contactStatus.userName,
                        fontSize = 12.sp,
                        color = if (contactStatus.hasUnseen) TextPrimary else TextSecondary,
                        fontWeight = if (contactStatus.hasUnseen) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(chat.avatarColorHex)),
            contentAlignment = Alignment.Center
        ) {
            when (chat.type) {
                ChatType.SAVED_VAULT -> Icon(
                    Icons.Default.Bookmark,
                    contentDescription = "Vault",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                ChatType.CHANNEL -> Icon(
                    Icons.Default.Campaign,
                    contentDescription = "Channel",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                ChatType.SECRET -> Icon(
                    Icons.Default.Lock,
                    contentDescription = "Secret",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                else -> Text(
                    text = chat.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (chat.isVerified) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Session",
                            tint = SessionCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (chat.disappearingTimer != DisappearingTimer.OFF) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Disappearing",
                            tint = SessionNeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = chat.lastTimestamp,
                    fontSize = 12.sp,
                    color = if (chat.unreadCount > 0) TelegramBlue else TextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (chat.type == ChatType.SAVED_VAULT) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = TelegramBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = chat.lastMessage,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TelegramBlue)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else if (chat.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
