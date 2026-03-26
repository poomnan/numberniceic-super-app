package com.numberniceic.data.admin

import com.google.gson.annotations.SerializedName

data class ApplicationPrivilege(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("detail") val detail: String,
    @SerializedName("benefits") val benefits: String,
    @SerializedName("price") val price: String
)
