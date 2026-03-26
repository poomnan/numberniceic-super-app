package com.numberniceic.data.admin

import com.google.gson.annotations.SerializedName

data class SpellItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String?,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("type") val type: String,
    @SerializedName("note") val note: String? = ""
)

data class SpellListResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<SpellItem>
)
