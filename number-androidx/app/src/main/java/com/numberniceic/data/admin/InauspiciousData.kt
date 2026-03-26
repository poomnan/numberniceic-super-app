package com.numberniceic.data.admin

import com.google.gson.annotations.SerializedName

data class InauspiciousData(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String, // 'year' or 'life'
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("assigned_at") val assignedAt: String?
)
