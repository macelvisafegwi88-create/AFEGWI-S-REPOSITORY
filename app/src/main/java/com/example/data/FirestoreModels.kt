package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val sessionId: String = "",
    val recoverySeed: List<String> = emptyList(),
    val onionRoutingEnabled: Boolean = true,
    val dummyTrafficEnabled: Boolean = true,
    val screenSecurityEnabled: Boolean = true,
    val isDecoyMode: Boolean = false,
    val totalVaultSize: String = "0 MB",
    val totalChunksEncrypted: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMetaEntity(
    @DocumentId val docId: String = "",
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val recipientSessionId: String = "",
    val type: String = "DIRECT",
    val lastMessage: String = "",
    val lastTimestamp: String = "",
    val unreadCount: Long = 0L,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isVerified: Boolean = true,
    val disappearingTimer: String = "OFF",
    val safetyNumber: String = "",
    val avatarColorHex: Long = 0xFF2AABEE,
    val onlineStatus: String = "Active Node",
    val memberCount: Long = 1L,
    val topic: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessageEntity(
    @DocumentId val docId: String = "",
    val id: String = "",
    val userId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isOutgoing: Boolean = true,
    val status: String = "DELIVERED",
    val attachmentName: String? = null,
    val attachmentFormattedSize: String? = null,
    val attachmentType: String? = null,
    val attachmentChecksum: String? = null,
    val isVoice: Boolean = false,
    val voiceDurationSec: Long = 0L,
    val isPinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StatusStoryEntity(
    @DocumentId val docId: String = "",
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userSessionId: String = "",
    val type: String = "TEXT_GRADIENT",
    val text: String = "",
    val caption: String = "",
    val durationHours: Long = 24L,
    val expiresAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
