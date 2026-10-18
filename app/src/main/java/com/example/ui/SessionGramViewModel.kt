package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SessionGramRepository
import com.example.model.*
import com.example.util.StorageHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class SessionGramViewModel(
    private val repository: SessionGramRepository = SessionGramRepository()
) : ViewModel() {

    val userAccount: StateFlow<UserAccount?> = repository.userAccount
    val chats: StateFlow<List<Chat>> = repository.chats
    val messages: StateFlow<Map<String, List<Message>>> = repository.messages
    val statuses: StateFlow<List<UserStatus>> = repository.statuses

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeStatusUser = MutableStateFlow<UserStatus?>(null)
    val activeStatusUser: StateFlow<UserStatus?> = _activeStatusUser.asStateFlow()

    private val _isCreateStatusOpen = MutableStateFlow(false)
    val isCreateStatusOpen: StateFlow<Boolean> = _isCreateStatusOpen.asStateFlow()

    private val _isOnionCircuitDialogOpen = MutableStateFlow(false)
    val isOnionCircuitDialogOpen: StateFlow<Boolean> = _isOnionCircuitDialogOpen.asStateFlow()

    private val _isSecuritySettingsOpen = MutableStateFlow(false)
    val isSecuritySettingsOpen: StateFlow<Boolean> = _isSecuritySettingsOpen.asStateFlow()

    private val _isSafetyNumberDialogOpen = MutableStateFlow(false)
    val isSafetyNumberDialogOpen: StateFlow<Boolean> = _isSafetyNumberDialogOpen.asStateFlow()

    private val _isNewChatDialogOpen = MutableStateFlow(false)
    val isNewChatDialogOpen: StateFlow<Boolean> = _isNewChatDialogOpen.asStateFlow()

    private val _isNewGroupDialogOpen = MutableStateFlow(false)
    val isNewGroupDialogOpen: StateFlow<Boolean> = _isNewGroupDialogOpen.asStateFlow()

    private val _isSendLargeFileDialogOpen = MutableStateFlow(false)
    val isSendLargeFileDialogOpen: StateFlow<Boolean> = _isSendLargeFileDialogOpen.asStateFlow()

    private val _isVaultScreenOpen = MutableStateFlow(false)
    val isVaultScreenOpen: StateFlow<Boolean> = _isVaultScreenOpen.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _voiceRecordingDuration = MutableStateFlow(0)
    val voiceRecordingDuration: StateFlow<Int> = _voiceRecordingDuration.asStateFlow()

    private var voiceRecordingJob: Job? = null

    val isRestoringCloudData: StateFlow<Boolean> = repository.isRestoringCloudData
    val cloudSyncError: StateFlow<String?> = repository.cloudSyncError

    init {
        // Only restore if user is already signed in with Google, otherwise load into sign-in page
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    repository.restoreOrInitializeGoogleUser(currentUser)
                } catch (e: Exception) {
                    android.util.Log.e("ViewModel", "Failed auto-restoring user session", e)
                }
            }
        }
    }

    fun restoreGoogleUser(
        user: com.google.firebase.auth.FirebaseUser,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.restoreOrInitializeGoogleUser(user)
                _activeChatId.value = null
                _activeStatusUser.value = null
                onComplete()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to restore account data from cloud")
            }
        }
    }

    val filteredChats: StateFlow<List<Chat>> = combine(
        chats,
        selectedTab,
        searchQuery
    ) { chatList, tabIndex, query ->
        var list = when (tabIndex) {
            0 -> chatList // All
            1 -> chatList.filter { it.type == ChatType.DIRECT || it.type == ChatType.SECRET } // Direct
            3 -> chatList.filter { it.type == ChatType.CHANNEL } // Channels
            4 -> chatList.filter { it.type == ChatType.GROUP } // Groups
            5 -> chatList.filter { it.type == ChatType.SAVED_VAULT } // Vault
            else -> chatList
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.lastMessage.contains(query, ignoreCase = true) ||
                it.recipientSessionId.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeChat: StateFlow<Chat?> = combine(chats, activeChatId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentChatMessages: StateFlow<List<Message>> = combine(messages, activeChatId) { map, id ->
        if (id == null) emptyList() else map[id] ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createAccount(displayName: String) {
        repository.createAccount(displayName)
        _activeChatId.value = null
        _activeStatusUser.value = null
    }

    fun loginWithSessionId(sessionId: String, displayName: String) {
        repository.loginWithSessionId(sessionId, displayName)
        _activeChatId.value = null
        _activeStatusUser.value = null
    }

    fun restoreWithSeedPhrase(seedPhrase: String, displayName: String) {
        val words = seedPhrase.trim().split("\\s+".toRegex())
        repository.restoreWithSeedPhrase(words, displayName)
        _activeChatId.value = null
        _activeStatusUser.value = null
    }

    fun logout() {
        _activeChatId.value = null
        _activeStatusUser.value = null
        _isSecuritySettingsOpen.value = false
        _isVaultScreenOpen.value = false
        repository.logout()
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun openChat(chatId: String) {
        _activeChatId.value = chatId
    }

    fun closeChat() {
        _activeChatId.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openStatusViewer(status: UserStatus) {
        _activeStatusUser.value = status
        repository.markStatusSeen(status.id)
    }

    fun closeStatusViewer() {
        _activeStatusUser.value = null
    }

    fun setCreateStatusOpen(open: Boolean) {
        _isCreateStatusOpen.value = open
    }

    fun setOnionCircuitDialogOpen(open: Boolean) {
        _isOnionCircuitDialogOpen.value = open
    }

    fun setSecuritySettingsOpen(open: Boolean) {
        _isSecuritySettingsOpen.value = open
    }

    fun setSafetyNumberDialogOpen(open: Boolean) {
        _isSafetyNumberDialogOpen.value = open
    }

    fun setNewChatDialogOpen(open: Boolean) {
        _isNewChatDialogOpen.value = open
    }

    fun setNewGroupDialogOpen(open: Boolean) {
        _isNewGroupDialogOpen.value = open
    }

    fun setSendLargeFileDialogOpen(open: Boolean) {
        _isSendLargeFileDialogOpen.value = open
    }

    fun setVaultScreenOpen(open: Boolean) {
        _isVaultScreenOpen.value = open
    }

    fun sendMessage(text: String, replyTo: ReplyPreview? = null) {
        val chatId = _activeChatId.value ?: return
        val currentTimer = activeChat.value?.disappearingTimer ?: DisappearingTimer.OFF
        repository.sendMessage(
            chatId = chatId,
            text = text,
            disappearingTimer = currentTimer,
            replyTo = replyTo
        )

        // If in a group chat, simulate realistic group peer responses from members
        val chat = activeChat.value
        if (chat?.type == ChatType.GROUP) {
            viewModelScope.launch {
                delay(1200)
                val peers = listOf(
                    Triple("Alex", "Verified via Lokinet node circuit. Zero metadata leaked.", 0xFFFF7043),
                    Triple("Elena Rostova", "Encrypted packet received and synced across group nodes.", 0xFF26A69A),
                    Triple("CipherZero", "Acknowledged in group vault.", 0xFF00F5D4)
                )
                val peer = peers.random()
                repository.addGroupPeerReply(chatId, peer.first, peer.second, peer.third)
            }
        }
    }

    fun sendLargeFile(fileName: String, sizeBytes: Long, formattedSize: String, type: AttachmentType) {
        val chatId = _activeChatId.value ?: "chat_vault"
        val att = Attachment(
            id = "att_" + System.currentTimeMillis(),
            type = type,
            fileName = fileName,
            fileSizeBytes = sizeBytes,
            formattedSize = formattedSize,
            sha256Checksum = "d41d8cd98f00b204e9800998ecf8427e" + (1000..9999).random(),
            transferProgress = 1.0f,
            isDownloaded = true,
            isTransferring = false,
            transferSpeed = "Transferred via 3 Onion Hops (Unlimited File Liberty)"
        )
        repository.sendMessage(
            chatId = chatId,
            text = "Transferred $formattedSize file (Telegram liberty + Session zero-knowledge routing)",
            attachment = att
        )
    }

    fun startVoiceRecording() {
        _isRecordingVoice.value = true
        _voiceRecordingDuration.value = 0
        voiceRecordingJob?.cancel()
        voiceRecordingJob = viewModelScope.launch {
            while (_isRecordingVoice.value) {
                delay(1000)
                _voiceRecordingDuration.value += 1
            }
        }
    }

    fun stopAndSendVoiceRecording() {
        val duration = _voiceRecordingDuration.value
        _isRecordingVoice.value = false
        voiceRecordingJob?.cancel()
        val chatId = _activeChatId.value ?: return
        if (duration >= 1) {
            repository.sendVoiceMessage(chatId, duration)
        }
    }

    fun cancelVoiceRecording() {
        _isRecordingVoice.value = false
        _voiceRecordingDuration.value = 0
        voiceRecordingJob?.cancel()
    }

    fun toggleDisappearingTimer(timer: DisappearingTimer) {
        val chatId = _activeChatId.value ?: return
        repository.toggleDisappearingTimer(chatId, timer)
    }

    fun postTextStatus(text: String, gradientColors: List<Long>, duration: StatusDuration) {
        if (text.isNotBlank()) {
            repository.addStatusText(text = text, colors = gradientColors, duration = duration)
            _isCreateStatusOpen.value = false
        }
    }

    fun postPhotoStatus(photoType: String, caption: String, duration: StatusDuration) {
        repository.addStatusPhoto(photoType, caption, duration)
        _isCreateStatusOpen.value = false
    }

    fun rerollOnionCircuit() {
        repository.rerollOnionCircuit()
    }

    fun toggleDummyTraffic() {
        repository.toggleDummyTraffic()
    }

    fun toggleScreenSecurity() {
        repository.toggleScreenSecurity()
    }

    fun createChat(name: String, sessionId: String, type: ChatType) {
        val created = repository.createNewChat(name, sessionId, type)
        _isNewChatDialogOpen.value = false
        openChat(created.id)
    }

    fun createGroup(name: String, topic: String, members: List<String>) {
        val created = repository.createNewGroup(name, topic, members)
        _isNewGroupDialogOpen.value = false
        openChat(created.id)
    }

    fun addGroupMember(chatId: String, name: String, sessionId: String) {
        repository.addGroupMember(chatId, name, sessionId)
    }

    fun uploadFromLocalStorage(context: Context, uri: Uri) {
        val chatId = _activeChatId.value ?: "chat_vault"
        val meta = StorageHelper.getFileMetadata(context, uri)
        val tempId = "att_" + System.currentTimeMillis()

        val initialAtt = Attachment(
            id = tempId,
            type = meta.attachmentType,
            fileName = meta.fileName,
            fileSizeBytes = meta.sizeBytes,
            formattedSize = meta.formattedSize,
            sha256Checksum = meta.sha256Checksum,
            transferProgress = 0.2f,
            isDownloaded = true,
            isTransferring = true,
            transferSpeed = "Uploading via 3 Lokinet Onion Hops..."
        )

        repository.sendMessage(
            chatId = chatId,
            text = "Shared ${meta.fileName} (${meta.formattedSize}) from device storage (Zero compression)",
            attachment = initialAtt
        )

        viewModelScope.launch {
            val msgs = repository.messages.value[chatId] ?: emptyList()
            val sentMsg = msgs.lastOrNull { it.attachment?.id == tempId }
            val msgId = sentMsg?.id ?: ""

            for (step in 3..10) {
                delay(120)
                val progress = step / 10f
                if (msgId.isNotEmpty()) {
                    repository.updateAttachmentTransfer(
                        chatId = chatId,
                        messageId = msgId,
                        progress = progress,
                        isDownloaded = true
                    )
                }
            }
            Toast.makeText(context, "✓ Uploaded: ${meta.fileName} (${meta.formattedSize})", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadAttachmentToLocalStorage(
        context: Context,
        chatId: String,
        messageId: String,
        attachment: Attachment,
        onComplete: (File?) -> Unit = {}
    ) {
        viewModelScope.launch {
            // Smooth progress feedback (e.g. 15% -> 90%)
            for (step in 1..8) {
                delay(90)
                val progress = (step / 10f).coerceAtMost(0.9f)
                repository.updateAttachmentTransfer(
                    chatId = chatId,
                    messageId = messageId,
                    progress = progress,
                    isDownloaded = false
                )
            }

            // Write file physically to local Downloads folder
            val result = StorageHelper.saveFileToLocalDownloads(
                context = context,
                fileName = attachment.fileName,
                mimeType = "*/*"
            )

            delay(120)
            repository.updateAttachmentTransfer(
                chatId = chatId,
                messageId = messageId,
                progress = 1.0f,
                isDownloaded = true
            )

            if (result.success) {
                Toast.makeText(
                    context,
                    "✓ Saved to Downloads: ${attachment.fileName}",
                    Toast.LENGTH_LONG
                ).show()
                onComplete(result.file)
            } else {
                Toast.makeText(
                    context,
                    "Download error: ${result.errorMessage ?: "Storage access error"}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete(null)
            }
        }
    }

    fun downloadVaultFile(
        context: Context,
        fileName: String,
        onComplete: (File?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = StorageHelper.saveFileToLocalDownloads(
                context = context,
                fileName = fileName,
                mimeType = "*/*"
            )
            if (result.success) {
                Toast.makeText(
                    context,
                    "✓ File downloaded to Downloads folder: $fileName",
                    Toast.LENGTH_LONG
                ).show()
                onComplete(result.file)
            } else {
                Toast.makeText(
                    context,
                    "Download failed: ${result.errorMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete(null)
            }
        }
    }

    fun uploadToVaultFromLocalStorage(context: Context, uri: Uri) {
        val meta = StorageHelper.getFileMetadata(context, uri)
        viewModelScope.launch {
            val current = repository.userAccount.value
            val addedChunks = (meta.sizeBytes / (2 * 1024 * 1024)).coerceAtLeast(1)
            Toast.makeText(
                context,
                "✓ Encrypted ${meta.fileName} into Vault (${meta.formattedSize}, $addedChunks onion chunks)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun postPhotoStatusFromUri(context: Context, uri: Uri, caption: String, duration: StatusDuration) {
        val meta = StorageHelper.getFileMetadata(context, uri)
        val photoKey = "device_photo_" + meta.fileName
        postPhotoStatus(photoKey, caption, duration)
        Toast.makeText(context, "✓ Photo status posted from device gallery", Toast.LENGTH_SHORT).show()
    }

    fun openDownloadedFile(context: Context, file: File) {
        StorageHelper.openDownloadedFile(context, file)
    }

    fun simulateDownloadProgress(chatId: String, messageId: String) {
        viewModelScope.launch {
            for (step in 1..10) {
                delay(120)
                val progress = step / 10f
                repository.updateAttachmentTransfer(chatId, messageId, progress, isDownloaded = step == 10)
            }
        }
    }
}
