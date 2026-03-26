package com.numberniceic.data.notification

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("member_id") val memberId: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("read_at") val readAt: String?,
    @SerializedName("created_at") val createdAt: String
)

data class NotificationListResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<NotificationResponse>,
    @SerializedName("count") val count: Int,
    @SerializedName("type") val type: String? = null
)

data class UnreadCountResponse(
    @SerializedName("status") val status: String,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("vipcode") val vipCode: String? = null
)

data class MarkReadRequest(
    @SerializedName("memberid") val memberId: String,
    @SerializedName("notification_id") val notificationId: Int? = null,
    @SerializedName("notification_ids") val notificationIds: List<Int>? = null
)
