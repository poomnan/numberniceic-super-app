package com.numberniceic.data.chat

import com.google.gson.annotations.SerializedName

data class ChatSession(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("guest_name") val guestName: String,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("last_message_at") val lastMessageAt: String
)

data class ChatMessage(
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("sender_type") val senderType: String, // 'customer' or 'admin'
    @SerializedName("sender_name") val senderName: String? = null, // Name of the sender (nullable for backward compatibility)
    @SerializedName("sender_avatar") val senderAvatar: String? = null, // Avatar ID/String from server
    @SerializedName("message") val message: String,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("vip_status") val vipStatus: String? = null,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String,
    // Local State Fields (Excluded from Server JSON by virtue of not being Sent)
    var isSending: Boolean = false,
    var isFailed: Boolean = false
)
