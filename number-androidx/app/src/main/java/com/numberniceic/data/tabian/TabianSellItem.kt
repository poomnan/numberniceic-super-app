package com.numberniceic.data.tabian

import com.google.gson.annotations.SerializedName

data class TabianSellItem(
    @SerializedName("tabian_id") val tabianId: Int? = 0,
    @SerializedName("tabian_number") val tabianNumber: String? = "",
    @SerializedName("tabian_province") val tabianProvince: String? = "",
    @SerializedName("tabian_price") val tabianPrice: Int? = 0,
    @SerializedName("tabian_status") val tabianStatus: String? = "available",
    @SerializedName("tabian_category") val tabianCategory: String? = "",
    @SerializedName("tabian_tag") val tabianTag: String? = "",
    @SerializedName("order_no") val orderNo: Int? = 0,
    @SerializedName("created_at") val createdAt: String? = "",
    @SerializedName("updated_at") val updatedAt: String? = ""
)
