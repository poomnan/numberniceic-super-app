package com.numberniceic.data.naming

import com.google.gson.annotations.SerializedName

data class NamingChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("guest_id") val guestId: String? = null,
    @SerializedName("member_id") val memberId: String? = null,
    @SerializedName("history") val history: List<Map<String, String>> = emptyList()
)

data class NamingChatResponse(
    @SerializedName("reply") val reply: String,
    @SerializedName("data") val data: List<NameMiracle>?
)

data class NameMiracle(
    @SerializedName("id") val id: Int,
    @SerializedName("thname") val thName: String, 
    @SerializedName("meaning") val meaning: String,
    @SerializedName("sat_sum") val satSum: Int,
    @SerializedName("sha_sum") val shaSum: Int,
    @SerializedName("sat_grade") val satGrade: String,
    @SerializedName("sha_grade") val shaGrade: String,
    @SerializedName("sat_desc") val satDesc: String,
    @SerializedName("sha_desc") val shaDesc: String,
    @SerializedName("sat_pairs") val satPairs: List<String>?,
    @SerializedName("sha_pairs") val shaPairs: List<String>?
)
