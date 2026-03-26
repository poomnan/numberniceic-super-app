package com.numberniceic.data.admin

import com.google.gson.annotations.SerializedName

data class BuddhaPang(
    @SerializedName("id") val id: Int? = 0,
    @SerializedName("pang_name") val pangName: String? = "",
    @SerializedName("buddha_day") val buddhaDay: Int? = 0,
    @SerializedName("description") val description: String? = "",
    @SerializedName("custom_description") val customDescription: String? = "",
    @SerializedName("image_url") val imageUrl: String? = ""
)
