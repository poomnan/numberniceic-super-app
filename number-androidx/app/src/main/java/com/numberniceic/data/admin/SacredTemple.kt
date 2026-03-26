package com.numberniceic.data.admin

import com.google.gson.annotations.SerializedName

data class SacredTemple(
    @SerializedName("id") val id: Int? = 0,
    @SerializedName("temple_name") val templeName: String? = "",
    @SerializedName("description") val description: String? = "",
    @SerializedName("image_url") val imageUrl: String? = "",
    @SerializedName("address") val address: String? = "",
    @SerializedName("assign_desc") val assignDesc: String? = "",
    @SerializedName("assigned_at") val assignedAt: String? = ""
)
