package com.example.model

enum class ChatType {
    DIRECT,
    GROUP,
    CHANNEL,
    SAVED_VAULT,
    SECRET
}

enum class DisappearingTimer(val label: String, val seconds: Long) {
    OFF("Off", 0L),
    SEC_10("10 seconds", 10L),
    MIN_1("1 minute", 60L),
    HOUR_1("1 hour", 3600L),
    DAY_1("24 hours", 86400L),
    WEEK_1("1 week", 604800L)
}

enum class StatusDuration(val label: String, val hours: Int, val shortLabel: String) {
    HOURS_24("24 Hours", 24, "24h"),
    DAYS_7("7 Days", 168, "7d"),
    DAYS_30("30 Days", 720, "30d")
}

enum class MessageStatus {
    SENDING,
    SENT_ONION,
    DELIVERED,
    READ
}

enum class AttachmentType {
    LARGE_FILE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    IMAGE
}

data class Attachment(
    val id: String,
    val type: AttachmentType,
    val fileName: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val sha256Checksum: String,
    val transferProgress: Float = 1.0f,
    val isDownloaded: Boolean = true,
    val isTransferring: Boolean = false,
    val transferSpeed: String = "48.2 MB/s via 3 Onion Hops",
    val mediaUrl: String? = null
)

data class ReplyPreview(
    val senderName: String,
    val snippet: String
)

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val status: MessageStatus = MessageStatus.DELIVERED,
    val attachment: Attachment? = null,
    val isVoice: Boolean = false,
    val voiceDurationSec: Int = 0,
    val voiceWaveform: List<Float> = emptyList(),
    val disappearingTimer: DisappearingTimer = DisappearingTimer.OFF,
    val expiresAt: Long? = null,
    val replyTo: ReplyPreview? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val isPinned: Boolean = false,
    val senderColorHex: Long = 0xFF2AABEE
)

data class GroupMember(
    val name: String,
    val sessionId: String,
    val role: String = "Member", // "Admin" or "Member"
    val avatarColorHex: Long = 0xFF2AABEE
)

data class Chat(
    val id: String,
    val name: String,
    val recipientSessionId: String,
    val type: ChatType,
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isVerified: Boolean = true,
    val disappearingTimer: DisappearingTimer = DisappearingTimer.OFF,
    val safetyNumber: String = "",
    val avatarColorHex: Long = 0xFF2AABEE,
    val onlineStatus: String = "Decentralized Node Active",
    val memberCount: Int = 1,
    val topic: String = "",
    val members: List<GroupMember> = emptyList()
)

data class OnionHop(
    val hopIndex: Int,
    val role: String,
    val location: String,
    val ipMask: String,
    val latencyMs: Int,
    val verified: Boolean = true
)

data class UserAccount(
    val sessionId: String,
    val displayName: String,
    val recoverySeed: List<String>,
    val onionRoutingEnabled: Boolean = true,
    val dummyTrafficEnabled: Boolean = true,
    val screenSecurityEnabled: Boolean = true,
    val activeHops: List<OnionHop> = emptyList(),
    val isDecoyMode: Boolean = false,
    val totalVaultSize: String = "0 MB",
    val totalChunksEncrypted: Long = 0L,
    val isLoggedIn: Boolean = false
)

enum class StatusType {
    TEXT_GRADIENT,
    PHOTO
}

data class StatusViewer(
    val name: String,
    val sessionId: String,
    val viewedAt: String
)

data class StatusItem(
    val id: String,
    val type: StatusType,
    val text: String = "",
    val gradientColors: List<Long> = emptyList(),
    val caption: String = "",
    val timestamp: String,
    val createdAt: Long = System.currentTimeMillis(),
    val duration: StatusDuration = StatusDuration.HOURS_24,
    val expiresAt: Long = System.currentTimeMillis() + (24 * 3600 * 1000L),
    val viewers: List<StatusViewer> = emptyList(),
    val photoPlaceholderType: String = "" // e.g. "cyber_setup", "city_night", "code_terminal"
)

data class UserStatus(
    val id: String,
    val userId: String,
    val userName: String,
    val userSessionId: String,
    val isMyStatus: Boolean = false,
    val avatarColorHex: Long = 0xFF2AABEE,
    val items: List<StatusItem>,
    val hasUnseen: Boolean = true
)
