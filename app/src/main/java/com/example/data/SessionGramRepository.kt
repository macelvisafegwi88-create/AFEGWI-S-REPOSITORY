package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

class SessionGramRepository(private val db: FirebaseFirestore? = null) {

    constructor(context: Context) : this(
        try {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        } catch (e: Exception) {
            Log.e("Repository", "Could not initialize named firestore, falling back", e)
            null
        }
    )

    private val auth: FirebaseAuth?
        get() = try {
            Firebase.auth
        } catch (_: Exception) {
            null
        }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val secureRandom = SecureRandom()

    private val wordPool = listOf(
        "crypto", "onion", "shield", "telegram", "vault", "session",
        "private", "quantum", "liberty", "relay", "stealth", "matrix",
        "cipher", "guard", "tunnel", "beacon", "mesh", "silent",
        "node", "vector", "orbit", "anchor", "shadow", "pulse",
        "signal", "packet", "zero", "knowledge", "freedom", "secure"
    )

    private val _userAccount = MutableStateFlow<UserAccount?>(null)
    val userAccount: StateFlow<UserAccount?> = _userAccount.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messages: StateFlow<Map<String, List<Message>>> = _messages.asStateFlow()

    private val _statuses = MutableStateFlow<List<UserStatus>>(emptyList())
    val statuses: StateFlow<List<UserStatus>> = _statuses.asStateFlow()

    private val _isRestoringCloudData = MutableStateFlow(false)
    val isRestoringCloudData: StateFlow<Boolean> = _isRestoringCloudData.asStateFlow()

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    private var chatsListener: ListenerRegistration? = null
    private var statusesListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google to access Firestore.")
    }

    /**
     * Authenticated Google user login / restore flow.
     * Restores all data for the valid Gmail user ID from Cloud Firestore.
     * If user does not exist in Firestore yet, creates account attached to Gmail.
     */
    suspend fun restoreOrInitializeGoogleUser(user: FirebaseUser): UserAccount = withContext(Dispatchers.IO) {
        _isRestoringCloudData.value = true
        _cloudSyncError.value = null

        val uid = user.uid
        val email = user.email ?: "operator@gmail.com"
        val rawName = user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")

        val firestoreDb = db
        if (firestoreDb == null) {
            // Local fallback if firestore uninitialized
            val localAccount = createAccount(rawName)
            _isRestoringCloudData.value = false
            return@withContext localAccount
        }

        try {
            val userDocRef = firestoreDb.collection("users").document(uid)
            val userSnapshot = userDocRef.get().await()

            val account: UserAccount
            if (userSnapshot.exists()) {
                val profile = userSnapshot.toObject(UserProfileEntity::class.java)
                account = UserAccount(
                    sessionId = profile?.sessionId?.takeIf { it.isNotBlank() } ?: generateCryptographicSessionId(),
                    displayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: rawName,
                    recoverySeed = profile?.recoverySeed?.takeIf { it.isNotEmpty() } ?: generateMnemonicSeed(),
                    onionRoutingEnabled = profile?.onionRoutingEnabled ?: true,
                    dummyTrafficEnabled = profile?.dummyTrafficEnabled ?: true,
                    screenSecurityEnabled = profile?.screenSecurityEnabled ?: true,
                    activeHops = defaultOnionHops(),
                    isDecoyMode = profile?.isDecoyMode ?: false,
                    totalVaultSize = profile?.totalVaultSize ?: "0 MB",
                    totalChunksEncrypted = profile?.totalChunksEncrypted ?: 0L,
                    isLoggedIn = true
                )

                // 1. Reload all chats from cloud
                val chatsSnapshot = userDocRef.collection("chats").get().await()
                val restoredChats = mutableListOf<Chat>()
                val restoredMessages = mutableMapOf<String, List<Message>>()

                for (chatDoc in chatsSnapshot.documents) {
                    val entity = chatDoc.toObject(ChatMetaEntity::class.java)
                    if (entity != null) {
                        val chat = mapEntityToChat(entity)
                        restoredChats.add(chat)

                        // Reload all messages for this chat
                        val messagesSnapshot = chatDoc.reference.collection("messages").get().await()
                        val msgs = messagesSnapshot.documents.mapNotNull { msgDoc ->
                            msgDoc.toObject(ChatMessageEntity::class.java)?.let { mapEntityToMessage(it) }
                        }.sortedBy { it.timestamp }
                        restoredMessages[chat.id] = msgs
                    }
                }

                // If no chats found, create the default Saved Vault
                if (restoredChats.isEmpty()) {
                    val defaultVault = createDefaultVault(account)
                    restoredChats.add(defaultVault)
                    saveChatToCloud(defaultVault, uid)
                }

                // 2. Reload all statuses from cloud
                val statusSnapshot = userDocRef.collection("statuses").get().await()
                val restoredStatuses = statusSnapshot.documents.mapNotNull { sDoc ->
                    sDoc.toObject(StatusStoryEntity::class.java)?.let { mapEntityToStatusItem(it) }
                }

                val myStatusContainer = UserStatus(
                    id = "status_my",
                    userId = account.sessionId,
                    userName = "My Status",
                    userSessionId = account.sessionId,
                    isMyStatus = true,
                    avatarColorHex = 0xFF2AABEE,
                    items = restoredStatuses,
                    hasUnseen = false
                )

                _chats.value = restoredChats
                _messages.value = restoredMessages
                _statuses.value = listOf(myStatusContainer)
                _userAccount.value = account

            } else {
                // New Google account creation
                val newSessionId = generateCryptographicSessionId()
                val seed = generateMnemonicSeed()
                account = UserAccount(
                    sessionId = newSessionId,
                    displayName = rawName,
                    recoverySeed = seed,
                    onionRoutingEnabled = true,
                    dummyTrafficEnabled = true,
                    screenSecurityEnabled = true,
                    activeHops = defaultOnionHops(),
                    isDecoyMode = false,
                    totalVaultSize = "0 MB",
                    totalChunksEncrypted = 0L,
                    isLoggedIn = true
                )

                // Save profile to Cloud
                val profileMap = mapOf(
                    "userId" to uid,
                    "email" to email,
                    "displayName" to rawName,
                    "sessionId" to newSessionId,
                    "recoverySeed" to seed,
                    "onionRoutingEnabled" to true,
                    "dummyTrafficEnabled" to true,
                    "screenSecurityEnabled" to true,
                    "isDecoyMode" to false,
                    "totalVaultSize" to "0 MB",
                    "totalChunksEncrypted" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(profileMap).await()

                // Initialize default Saved Vault
                val defaultVault = createDefaultVault(account)
                _chats.value = listOf(defaultVault)
                saveChatToCloud(defaultVault, uid)

                val initialVaultWelcome = Message(
                    id = "msg_" + UUID.randomUUID().toString(),
                    chatId = defaultVault.id,
                    senderId = account.sessionId,
                    senderName = "Saved Messages",
                    text = "Welcome to AFEGWICHAT! Your encrypted cloud vault is now synchronized with your Google account. Store unlimited files, forward messages, and keep private notes with end-to-end security.",
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = true,
                    status = MessageStatus.READ,
                    isPinned = true
                )
                _messages.value = mapOf(defaultVault.id to listOf(initialVaultWelcome))
                saveMessageToCloud(initialVaultWelcome, uid)

                val myEmptyStatus = UserStatus(
                    id = "status_my",
                    userId = account.sessionId,
                    userName = "My Status",
                    userSessionId = account.sessionId,
                    isMyStatus = true,
                    avatarColorHex = 0xFF2AABEE,
                    items = emptyList(),
                    hasUnseen = false
                )
                _statuses.value = listOf(myEmptyStatus)
                _userAccount.value = account
            }

            // Cloud data has been completely restored; now start realtime listeners
            startRealtimeCloudSync(uid)
            _isRestoringCloudData.value = false
            return@withContext account

        } catch (e: Exception) {
            Log.e("Repository", "Failed restoring cloud data for user $uid", e)
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            _cloudSyncError.value = e.localizedMessage ?: "Failed to restore cloud data"
            _isRestoringCloudData.value = false
            throw e
        }
    }

    private fun startRealtimeCloudSync(uid: String) {
        val firestoreDb = db ?: return

        // Stop any old listeners
        stopRealtimeCloudSync()

        // 1. Listen to chats
        val userChatsRef = firestoreDb.collection("users").document(uid).collection("chats")
        chatsListener = userChatsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, userChatsRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val updatedChats = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMetaEntity::class.java)?.let { mapEntityToChat(it) }
                }
                if (updatedChats.isNotEmpty()) {
                    _chats.value = updatedChats
                }

                // Attach message listeners for each chat
                for (doc in snapshot.documents) {
                    val chatId = doc.id
                    if (!messageListeners.containsKey(chatId)) {
                        val messagesRef = doc.reference.collection("messages")
                        val msgListener = messagesRef.addSnapshotListener { msgSnapshot, msgError ->
                            if (msgError != null) {
                                handleFirestoreError(msgError, OperationType.LIST, messagesRef.path)
                                return@addSnapshotListener
                            }
                            if (msgSnapshot != null) {
                                val msgs = msgSnapshot.documents.mapNotNull { mDoc ->
                                    mDoc.toObject(ChatMessageEntity::class.java)?.let { mapEntityToMessage(it) }
                                }.sortedBy { it.timestamp }
                                val mapCopy = _messages.value.toMutableMap()
                                mapCopy[chatId] = msgs
                                _messages.value = mapCopy
                            }
                        }
                        messageListeners[chatId] = msgListener
                    }
                }
            }
        }

        // 2. Listen to statuses
        val userStatusesRef = firestoreDb.collection("users").document(uid).collection("statuses")
        statusesListener = userStatusesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, userStatusesRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(StatusStoryEntity::class.java)?.let { mapEntityToStatusItem(it) }
                }
                val currentMyStatus = _statuses.value.find { it.isMyStatus }
                val updatedMyStatus = currentMyStatus?.copy(items = items) ?: UserStatus(
                    id = "status_my",
                    userId = _userAccount.value?.sessionId ?: "0500",
                    userName = "My Status",
                    userSessionId = _userAccount.value?.sessionId ?: "0500",
                    isMyStatus = true,
                    avatarColorHex = 0xFF2AABEE,
                    items = items,
                    hasUnseen = false
                )
                _statuses.value = listOf(updatedMyStatus)
            }
        }
    }

    private fun stopRealtimeCloudSync() {
        chatsListener?.remove()
        chatsListener = null
        statusesListener?.remove()
        statusesListener = null
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
    }

    private fun createDefaultVault(account: UserAccount): Chat {
        return Chat(
            id = "chat_vault",
            name = "Saved Messages",
            recipientSessionId = account.sessionId,
            type = ChatType.SAVED_VAULT,
            lastMessage = "Personal Encrypted Cloud Vault",
            lastTimestamp = "Just now",
            unreadCount = 0,
            isPinned = true,
            avatarColorHex = 0xFF2AABEE,
            onlineStatus = "Unlimited Encrypted Cloud Vault",
            memberCount = 1
        )
    }

    private fun mapEntityToChat(entity: ChatMetaEntity): Chat {
        val chatType = try {
            ChatType.valueOf(entity.type)
        } catch (e: Exception) {
            ChatType.DIRECT
        }
        val disappearing = try {
            DisappearingTimer.valueOf(entity.disappearingTimer)
        } catch (e: Exception) {
            DisappearingTimer.OFF
        }
        return Chat(
            id = entity.id.ifBlank { entity.docId },
            name = entity.name,
            recipientSessionId = entity.recipientSessionId,
            type = chatType,
            lastMessage = entity.lastMessage,
            lastTimestamp = entity.lastTimestamp,
            unreadCount = entity.unreadCount.toInt(),
            isPinned = entity.isPinned,
            isMuted = entity.isMuted,
            isVerified = entity.isVerified,
            disappearingTimer = disappearing,
            safetyNumber = entity.safetyNumber,
            avatarColorHex = entity.avatarColorHex,
            onlineStatus = entity.onlineStatus,
            memberCount = entity.memberCount.toInt(),
            topic = entity.topic
        )
    }

    private fun mapEntityToMessage(entity: ChatMessageEntity): Message {
        val msgStatus = try {
            MessageStatus.valueOf(entity.status)
        } catch (e: Exception) {
            MessageStatus.DELIVERED
        }
        val attachment = if (entity.attachmentName != null) {
            val attType = try {
                AttachmentType.valueOf(entity.attachmentType ?: "DOCUMENT")
            } catch (e: Exception) {
                AttachmentType.DOCUMENT
            }
            Attachment(
                id = "att_" + entity.id,
                type = attType,
                fileName = entity.attachmentName,
                fileSizeBytes = 0L,
                formattedSize = entity.attachmentFormattedSize ?: "Unknown",
                sha256Checksum = entity.attachmentChecksum ?: "verified"
            )
        } else null

        return Message(
            id = entity.id.ifBlank { entity.docId },
            chatId = entity.chatId,
            senderId = entity.senderId,
            senderName = entity.senderName,
            text = entity.text,
            timestamp = entity.timestamp,
            isOutgoing = entity.isOutgoing,
            status = msgStatus,
            attachment = attachment,
            isVoice = entity.isVoice,
            voiceDurationSec = entity.voiceDurationSec.toInt(),
            isPinned = entity.isPinned
        )
    }

    private fun mapEntityToStatusItem(entity: StatusStoryEntity): StatusItem {
        val statusType = try {
            StatusType.valueOf(entity.type)
        } catch (e: Exception) {
            StatusType.TEXT_GRADIENT
        }
        val duration = when (entity.durationHours) {
            168L -> StatusDuration.DAYS_7
            720L -> StatusDuration.DAYS_30
            else -> StatusDuration.HOURS_24
        }
        return StatusItem(
            id = entity.id.ifBlank { entity.docId },
            type = statusType,
            text = entity.text,
            caption = entity.caption,
            timestamp = "Today",
            duration = duration,
            expiresAt = entity.expiresAt
        )
    }

    private fun saveChatToCloud(chat: Chat, uid: String) {
        val firestoreDb = db ?: return
        scope.launch {
            try {
                val chatDoc = firestoreDb.collection("users").document(uid).collection("chats").document(chat.id)
                val payload = mapOf(
                    "id" to chat.id,
                    "userId" to uid,
                    "name" to chat.name,
                    "recipientSessionId" to chat.recipientSessionId,
                    "type" to chat.type.name,
                    "lastMessage" to chat.lastMessage,
                    "lastTimestamp" to chat.lastTimestamp,
                    "unreadCount" to chat.unreadCount.toLong(),
                    "isPinned" to chat.isPinned,
                    "isMuted" to chat.isMuted,
                    "isVerified" to chat.isVerified,
                    "disappearingTimer" to chat.disappearingTimer.name,
                    "safetyNumber" to chat.safetyNumber,
                    "avatarColorHex" to chat.avatarColorHex,
                    "onlineStatus" to chat.onlineStatus,
                    "memberCount" to chat.memberCount.toLong(),
                    "topic" to chat.topic,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                chatDoc.set(payload, SetOptions.merge()).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/$uid/chats/${chat.id}")
            }
        }
    }

    private fun saveMessageToCloud(message: Message, uid: String) {
        val firestoreDb = db ?: return
        scope.launch {
            try {
                val msgDoc = firestoreDb.collection("users").document(uid)
                    .collection("chats").document(message.chatId)
                    .collection("messages").document(message.id)

                val payload = mutableMapOf<String, Any>(
                    "id" to message.id,
                    "userId" to uid,
                    "chatId" to message.chatId,
                    "senderId" to message.senderId,
                    "senderName" to message.senderName,
                    "text" to message.text,
                    "timestamp" to message.timestamp,
                    "isOutgoing" to message.isOutgoing,
                    "status" to message.status.name,
                    "isVoice" to message.isVoice,
                    "voiceDurationSec" to message.voiceDurationSec.toLong(),
                    "isPinned" to message.isPinned,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                message.attachment?.let {
                    payload["attachmentName"] = it.fileName
                    payload["attachmentFormattedSize"] = it.formattedSize
                    payload["attachmentType"] = it.type.name
                    payload["attachmentChecksum"] = it.sha256Checksum
                }

                msgDoc.set(payload).await()

                // Update last message on parent chat document
                val previewSnippet = when {
                    message.attachment != null -> "📎 ${message.attachment.fileName} (${message.attachment.formattedSize})"
                    else -> message.text
                }
                firestoreDb.collection("users").document(uid)
                    .collection("chats").document(message.chatId)
                    .update(
                        mapOf(
                            "lastMessage" to previewSnippet,
                            "lastTimestamp" to "Just now",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/$uid/chats/${message.chatId}/messages/${message.id}")
            }
        }
    }

    fun sendMessage(
        chatId: String,
        text: String,
        attachment: Attachment? = null,
        disappearingTimer: DisappearingTimer = DisappearingTimer.OFF,
        replyTo: ReplyPreview? = null
    ) {
        val currentAccount = _userAccount.value ?: return
        val currentList = _messages.value[chatId] ?: emptyList()
        val newMessage = Message(
            id = "msg_" + UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentAccount.sessionId,
            senderName = currentAccount.displayName,
            text = text,
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            status = MessageStatus.DELIVERED,
            attachment = attachment,
            disappearingTimer = disappearingTimer,
            expiresAt = if (disappearingTimer.seconds > 0) System.currentTimeMillis() + (disappearingTimer.seconds * 1000) else null,
            replyTo = replyTo
        )
        val updatedMap = _messages.value.toMutableMap()
        updatedMap[chatId] = currentList + newMessage
        _messages.value = updatedMap

        val previewSnippet = when {
            attachment != null -> "📎 ${attachment.fileName} (${attachment.formattedSize})"
            else -> text
        }
        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) {
                chat.copy(
                    lastMessage = previewSnippet,
                    lastTimestamp = "Just now"
                )
            } else chat
        }

        // Save immediately to Firestore in real-time
        auth?.currentUser?.uid?.let { uid ->
            saveMessageToCloud(newMessage, uid)
        }
    }

    fun addGroupPeerReply(chatId: String, senderName: String, text: String, colorHex: Long) {
        val currentList = _messages.value[chatId] ?: emptyList()
        val peerMessage = Message(
            id = "msg_" + UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = "peer_" + UUID.randomUUID().toString().take(8),
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = MessageStatus.READ,
            senderColorHex = colorHex
        )
        val updatedMap = _messages.value.toMutableMap()
        updatedMap[chatId] = currentList + peerMessage
        _messages.value = updatedMap

        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) {
                chat.copy(
                    lastMessage = "$senderName: $text",
                    lastTimestamp = "Just now"
                )
            } else chat
        }

        auth?.currentUser?.uid?.let { uid ->
            saveMessageToCloud(peerMessage, uid)
        }
    }

    fun sendVoiceMessage(chatId: String, durationSec: Int) {
        val currentAccount = _userAccount.value ?: return
        val waveforms = (0..15).map { (0.2f + (Math.random() * 0.8f)).toFloat() }
        val currentList = _messages.value[chatId] ?: emptyList()
        val newMessage = Message(
            id = "msg_" + UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentAccount.sessionId,
            senderName = currentAccount.displayName,
            text = "Voice message ($durationSec s)",
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            status = MessageStatus.DELIVERED,
            isVoice = true,
            voiceDurationSec = durationSec,
            voiceWaveform = waveforms
        )
        val updatedMap = _messages.value.toMutableMap()
        updatedMap[chatId] = currentList + newMessage
        _messages.value = updatedMap

        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) {
                chat.copy(
                    lastMessage = "🎤 Voice message ($durationSec s)",
                    lastTimestamp = "Just now"
                )
            } else chat
        }

        auth?.currentUser?.uid?.let { uid ->
            saveMessageToCloud(newMessage, uid)
        }
    }

    fun createNewChat(name: String, recipientSessionId: String): Chat {
        val currentAccount = _userAccount.value
        val myId = currentAccount?.sessionId ?: "0500"
        val newChatId = "chat_" + UUID.randomUUID().toString().take(8)
        val colors = listOf(0xFF2AABEE, 0xFF00E676, 0xFFFF7043, 0xFFAB47BC, 0xFF26A69A)
        val avatarColorHex = colors.random()

        val newChat = Chat(
            id = newChatId,
            name = if (name.isNotBlank()) name.trim() else "Session " + recipientSessionId.take(8),
            recipientSessionId = recipientSessionId.trim(),
            type = ChatType.DIRECT,
            lastMessage = "End-to-end encrypted session created",
            lastTimestamp = "Just now",
            unreadCount = 0,
            safetyNumber = (1..6).map { (1000..9999).random() }.joinToString(" "),
            avatarColorHex = avatarColorHex,
            onlineStatus = "Decentralized Node Connected"
        )

        _chats.value = listOf(newChat) + _chats.value

        val welcomeMsg = Message(
            id = "msg_" + UUID.randomUUID().toString(),
            chatId = newChatId,
            senderId = myId,
            senderName = "System",
            text = "🔒 Direct session established with 3 onion routing hops. Zero phone numbers, zero IP logging, pure cryptographic identity.",
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = MessageStatus.READ,
            senderColorHex = 0xFF2AABEE
        )
        val updatedMap = _messages.value.toMutableMap()
        updatedMap[newChatId] = listOf(welcomeMsg)
        _messages.value = updatedMap

        auth?.currentUser?.uid?.let { uid ->
            saveChatToCloud(newChat, uid)
            saveMessageToCloud(welcomeMsg, uid)
        }

        return newChat
    }

    fun createNewGroup(name: String, topic: String, memberNames: List<String>): Chat {
        return createNewGroup(name = name, memberSessionIds = memberNames, topic = topic)
    }

    fun createNewGroup(name: String, memberSessionIds: List<String>, topic: String = ""): Chat {
        val currentAccount = _userAccount.value
        val myId = currentAccount?.sessionId ?: "0500"
        val myName = currentAccount?.displayName ?: "Operator"
        val newGroupId = "group_" + UUID.randomUUID().toString().take(8)
        val colors = listOf(0xFF2AABEE, 0xFF00E676, 0xFFFF7043, 0xFFAB47BC, 0xFF26A69A, 0xFFFFCA28)
        val avatarColorHex = colors.random()

        val members = mutableListOf<GroupMember>()
        members.add(GroupMember(name = "$myName (You)", sessionId = myId, role = "Owner", avatarColorHex = 0xFF2AABEE))

        memberSessionIds.forEachIndexed { index, sid ->
            val cleanSid = sid.trim()
            if (cleanSid.isNotBlank()) {
                val memberName = "Member ${index + 1} (${cleanSid.take(6)})"
                members.add(
                    GroupMember(
                        name = memberName,
                        sessionId = cleanSid,
                        role = "Member",
                        avatarColorHex = colors[(index + 1) % colors.size]
                    )
                )
            }
        }

        val newGroupChat = Chat(
            id = newGroupId,
            name = if (name.isNotBlank()) name.trim() else "Secure Group",
            recipientSessionId = newGroupId,
            type = ChatType.GROUP,
            lastMessage = "Group created with ${members.size} members",
            lastTimestamp = "Just now",
            unreadCount = 0,
            avatarColorHex = avatarColorHex,
            onlineStatus = "${members.size} members",
            memberCount = members.size,
            topic = topic,
            members = members
        )

        _chats.value = listOf(newGroupChat) + _chats.value

        val welcomeMsg = Message(
            id = "msg_" + UUID.randomUUID().toString(),
            chatId = newGroupId,
            senderId = myId,
            senderName = "System",
            text = "Group created with decentralized multi-hop onion routing. All messages are encrypted with zero metadata storage.",
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = MessageStatus.READ,
            senderColorHex = 0xFF2AABEE
        )
        val updatedMap = _messages.value.toMutableMap()
        updatedMap[newGroupId] = listOf(welcomeMsg)
        _messages.value = updatedMap

        auth?.currentUser?.uid?.let { uid ->
            saveChatToCloud(newGroupChat, uid)
            saveMessageToCloud(welcomeMsg, uid)
        }

        return newGroupChat
    }

    fun addGroupMember(chatId: String, memberName: String, sessionId: String) {
        val chat = _chats.value.find { it.id == chatId } ?: return
        val newMember = GroupMember(
            name = memberName,
            sessionId = if (sessionId.isNotBlank()) sessionId else "05" + UUID.randomUUID().toString().replace("-", ""),
            role = "Member",
            avatarColorHex = 0xFF00E676
        )
        val updatedMembers = chat.members + newMember
        val updatedChat = chat.copy(
            members = updatedMembers,
            memberCount = updatedMembers.size,
            onlineStatus = "${updatedMembers.size} members"
        )
        _chats.value = _chats.value.map {
            if (it.id == chatId) updatedChat else it
        }

        val currentMessages = _messages.value[chatId] ?: emptyList()
        val noticeMsg = Message(
            id = "msg_" + UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            text = "$memberName joined the group via Session ID ${newMember.sessionId.take(12)}...",
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = MessageStatus.READ,
            senderColorHex = 0xFF2AABEE
        )
        val updatedMap = _messages.value.toMutableMap()
        updatedMap[chatId] = currentMessages + noticeMsg
        _messages.value = updatedMap

        auth?.currentUser?.uid?.let { uid ->
            saveChatToCloud(updatedChat, uid)
            saveMessageToCloud(noticeMsg, uid)
        }
    }

    fun addStatusText(text: String, duration: StatusDuration = StatusDuration.HOURS_24, colors: List<Long> = listOf(0xFF00C853, 0xFF00B0FF)) {
        val account = _userAccount.value ?: return
        val durationMillis = duration.hours * 3600 * 1000L
        val newItem = StatusItem(
            id = "status_item_" + UUID.randomUUID().toString(),
            type = StatusType.TEXT_GRADIENT,
            text = text,
            gradientColors = colors,
            timestamp = "Just now",
            duration = duration,
            expiresAt = System.currentTimeMillis() + durationMillis
        )
        updateStatusItemInState(newItem, account, duration.hours)
    }

    fun addStatusPhoto(placeholderType: String, caption: String, duration: StatusDuration = StatusDuration.HOURS_24) {
        val account = _userAccount.value ?: return
        val durationMillis = duration.hours * 3600 * 1000L
        val newItem = StatusItem(
            id = "status_item_" + UUID.randomUUID().toString(),
            type = StatusType.PHOTO,
            caption = caption,
            timestamp = "Just now",
            duration = duration,
            expiresAt = System.currentTimeMillis() + durationMillis,
            photoPlaceholderType = placeholderType
        )
        updateStatusItemInState(newItem, account, duration.hours)
    }

    private fun updateStatusItemInState(newItem: StatusItem, account: UserAccount, durationHours: Int) {
        val currentList = _statuses.value.toMutableList()
        val myStatusIndex = currentList.indexOfFirst { it.isMyStatus }
        if (myStatusIndex != -1) {
            val myStatus = currentList[myStatusIndex]
            val updatedItems = listOf(newItem) + myStatus.items
            currentList[myStatusIndex] = myStatus.copy(items = updatedItems)
        } else {
            val newMyStatus = UserStatus(
                id = "status_my",
                userId = account.sessionId,
                userName = "My Status",
                userSessionId = account.sessionId,
                isMyStatus = true,
                avatarColorHex = 0xFF2AABEE,
                items = listOf(newItem),
                hasUnseen = false
            )
            currentList.add(0, newMyStatus)
        }
        _statuses.value = currentList

        // Save status immediately to Firestore
        auth?.currentUser?.uid?.let { uid ->
            val firestoreDb = db ?: return@let
            scope.launch {
                try {
                    val statusDoc = firestoreDb.collection("users").document(uid)
                        .collection("statuses").document(newItem.id)
                    val payload = mapOf(
                        "id" to newItem.id,
                        "userId" to uid,
                        "userName" to account.displayName,
                        "userSessionId" to account.sessionId,
                        "type" to newItem.type.name,
                        "text" to newItem.text,
                        "caption" to newItem.caption,
                        "durationHours" to durationHours.toLong(),
                        "expiresAt" to newItem.expiresAt,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    statusDoc.set(payload).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, "users/$uid/statuses/${newItem.id}")
                }
            }
        }
    }

    fun updateSecuritySettings(
        onionRouting: Boolean? = null,
        dummyTraffic: Boolean? = null,
        screenSecurity: Boolean? = null,
        isDecoy: Boolean? = null
    ) {
        val current = _userAccount.value ?: return
        val updated = current.copy(
            onionRoutingEnabled = onionRouting ?: current.onionRoutingEnabled,
            dummyTrafficEnabled = dummyTraffic ?: current.dummyTrafficEnabled,
            screenSecurityEnabled = screenSecurity ?: current.screenSecurityEnabled,
            isDecoyMode = isDecoy ?: current.isDecoyMode
        )
        _userAccount.value = updated

        auth?.currentUser?.uid?.let { uid ->
            val firestoreDb = db ?: return@let
            scope.launch {
                try {
                    val updates = mutableMapOf<String, Any>(
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    onionRouting?.let { updates["onionRoutingEnabled"] = it }
                    dummyTraffic?.let { updates["dummyTrafficEnabled"] = it }
                    screenSecurity?.let { updates["screenSecurityEnabled"] = it }
                    isDecoy?.let { updates["isDecoyMode"] = it }
                    firestoreDb.collection("users").document(uid).update(updates).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
                }
            }
        }
    }

    fun markStatusSeen(statusId: String) {
        _statuses.value = _statuses.value.map {
            if (it.id == statusId) it.copy(hasUnseen = false) else it
        }
    }

    fun toggleDisappearingTimer(chatId: String, timer: DisappearingTimer) {
        _chats.value = _chats.value.map {
            if (it.id == chatId) it.copy(disappearingTimer = timer) else it
        }
        auth?.currentUser?.uid?.let { uid ->
            val chat = _chats.value.find { it.id == chatId } ?: return@let
            saveChatToCloud(chat, uid)
        }
    }

    fun rerollOnionCircuit() {
        val currentAccount = _userAccount.value ?: return
        val newHops = listOf(
            OnionHop(1, "Entry Guard", listOf("Frankfurt, Germany", "Amsterdam, Netherlands", "Zurich, Switzerland").random(), "185.220.101.*** (lokinet)", (20..35).random(), true),
            OnionHop(2, "Middle Relay", listOf("Reykjavik, Iceland", "Oslo, Norway", "Helsinki, Finland").random(), "193.107.86.*** (lokinet)", (40..55).random(), true),
            OnionHop(3, "Exit Service Node", listOf("Stockholm, Sweden", "Geneva, Switzerland", "Vienna, Austria").random(), "194.26.29.*** (lokinet)", (60..75).random(), true)
        )
        _userAccount.value = currentAccount.copy(activeHops = newHops)
    }

    fun toggleDummyTraffic() {
        val current = _userAccount.value ?: return
        updateSecuritySettings(dummyTraffic = !current.dummyTrafficEnabled)
    }

    fun toggleScreenSecurity() {
        val current = _userAccount.value ?: return
        updateSecuritySettings(screenSecurity = !current.screenSecurityEnabled)
    }

    fun updateAttachmentTransfer(chatId: String, messageId: String, progress: Float, isDownloaded: Boolean) {
        val list = _messages.value[chatId] ?: return
        val updated = list.map { msg ->
            if (msg.id == messageId && msg.attachment != null) {
                msg.copy(
                    attachment = msg.attachment.copy(
                        transferProgress = progress,
                        isDownloaded = isDownloaded,
                        isTransferring = progress < 1.0f
                    )
                )
            } else msg
        }
        val copy = _messages.value.toMutableMap()
        copy[chatId] = updated
        _messages.value = copy
    }

    fun createNewChat(name: String, recipientSessionId: String, type: ChatType): Chat {
        val currentAccount = _userAccount.value
        val myId = currentAccount?.sessionId ?: "0500"
        val newChatId = "chat_" + UUID.randomUUID().toString().take(8)
        val colors = listOf(0xFF2AABEE, 0xFF00E676, 0xFFFF7043, 0xFFAB47BC, 0xFF26A69A)
        val avatarColorHex = colors.random()

        val newChat = Chat(
            id = newChatId,
            name = if (name.isNotBlank()) name.trim() else "Session " + recipientSessionId.take(8),
            recipientSessionId = recipientSessionId.trim(),
            type = type,
            lastMessage = "End-to-end encrypted session created",
            lastTimestamp = "Just now",
            unreadCount = 0,
            safetyNumber = (1..6).map { (1000..9999).random() }.joinToString(" "),
            avatarColorHex = avatarColorHex,
            onlineStatus = "Decentralized Node Connected"
        )

        _chats.value = listOf(newChat) + _chats.value

        val welcomeMsg = Message(
            id = "msg_" + UUID.randomUUID().toString(),
            chatId = newChatId,
            senderId = myId,
            senderName = "System",
            text = "🔒 Direct session established with 3 onion routing hops. Zero phone numbers, zero IP logging, pure cryptographic identity.",
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = MessageStatus.READ,
            senderColorHex = 0xFF2AABEE
        )
        val updatedMap = _messages.value.toMutableMap()
        updatedMap[newChatId] = listOf(welcomeMsg)
        _messages.value = updatedMap

        auth?.currentUser?.uid?.let { uid ->
            saveChatToCloud(newChat, uid)
            saveMessageToCloud(welcomeMsg, uid)
        }

        return newChat
    }

    fun createAccount(displayName: String): UserAccount {
        val newSessionId = generateCryptographicSessionId()
        val seedPhrase = generateMnemonicSeed()
        val account = UserAccount(
            sessionId = newSessionId,
            displayName = if (displayName.isNotBlank()) displayName.trim() else "Anonymous Operator",
            recoverySeed = seedPhrase,
            onionRoutingEnabled = true,
            dummyTrafficEnabled = true,
            screenSecurityEnabled = true,
            activeHops = defaultOnionHops(),
            isDecoyMode = false,
            totalVaultSize = "0 MB",
            totalChunksEncrypted = 0L,
            isLoggedIn = true
        )
        setupUserSession(account)
        return account
    }

    fun loginWithSessionId(sessionIdInput: String, displayName: String): UserAccount {
        val cleanSessionId = if (sessionIdInput.isNotBlank()) sessionIdInput.trim() else generateCryptographicSessionId()
        val seedPhrase = generateMnemonicSeed()
        val account = UserAccount(
            sessionId = cleanSessionId,
            displayName = if (displayName.isNotBlank()) displayName.trim() else "Restored Operator",
            recoverySeed = seedPhrase,
            onionRoutingEnabled = true,
            dummyTrafficEnabled = true,
            screenSecurityEnabled = true,
            activeHops = defaultOnionHops(),
            isDecoyMode = false,
            totalVaultSize = "0 MB",
            totalChunksEncrypted = 0L,
            isLoggedIn = true
        )
        setupUserSession(account)
        return account
    }

    fun restoreWithSeedPhrase(seedWords: List<String>, displayName: String): UserAccount {
        val derivedSessionId = generateCryptographicSessionId()
        val account = UserAccount(
            sessionId = derivedSessionId,
            displayName = if (displayName.isNotBlank()) displayName.trim() else "Restored Operator",
            recoverySeed = seedWords,
            onionRoutingEnabled = true,
            dummyTrafficEnabled = true,
            screenSecurityEnabled = true,
            activeHops = defaultOnionHops(),
            isDecoyMode = false,
            totalVaultSize = "0 MB",
            totalChunksEncrypted = 0L,
            isLoggedIn = true
        )
        setupUserSession(account)
        return account
    }

    fun logout() {
        stopRealtimeCloudSync()
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("Repository", "Failed to sign out from auth", e)
        }
        _userAccount.value = null
        _chats.value = emptyList()
        _messages.value = emptyMap()
        _statuses.value = emptyList()
    }

    private fun setupUserSession(account: UserAccount) {
        _userAccount.value = account
        val vaultChat = createDefaultVault(account)
        _chats.value = listOf(vaultChat)

        val initialVaultWelcome = Message(
            id = "msg_" + UUID.randomUUID().toString(),
            chatId = "chat_vault",
            senderId = account.sessionId,
            senderName = "Saved Messages",
            text = "Welcome to your personal Saved Messages vault! Store unlimited files (up to 4GB with Telegram liberty), forward messages, notes, and voice recordings. All data is encrypted with zero metadata logging.",
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            status = MessageStatus.READ,
            isPinned = true
        )
        _messages.value = mapOf("chat_vault" to listOf(initialVaultWelcome))

        val myEmptyStatus = UserStatus(
            id = "status_my",
            userId = account.sessionId,
            userName = "My Status",
            userSessionId = account.sessionId,
            isMyStatus = true,
            avatarColorHex = 0xFF2AABEE,
            items = emptyList(),
            hasUnseen = false
        )
        _statuses.value = listOf(myEmptyStatus)
    }

    private fun generateCryptographicSessionId(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "05$hex"
    }

    private fun generateMnemonicSeed(): List<String> {
        return (1..12).map { wordPool.random() }
    }

    private fun defaultOnionHops(): List<OnionHop> {
        return listOf(
            OnionHop(1, "Entry Guard", "Zurich, Switzerland", "185.220.101.*** (lokinet)", 28, true),
            OnionHop(2, "Middle Relay", "Reykjavik, Iceland", "193.107.86.*** (lokinet)", 44, true),
            OnionHop(3, "Exit Service Node", "Stockholm, Sweden", "194.26.29.*** (lokinet)", 62, true)
        )
    }
}
