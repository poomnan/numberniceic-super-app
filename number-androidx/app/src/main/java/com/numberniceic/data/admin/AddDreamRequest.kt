package com.numberniceic.data.admin

import com.google.gson.annotations.SerializedName

data class AddDreamRequest(
    @SerializedName("dream_keyword")
    val dreamKeyword: String,
    
    @SerializedName("dream_interpretation")
    val dreamInterpretation: String,
    
    @SerializedName("lucky_numbers")
    val luckyNumbers: String,

    @SerializedName("category")
    val category: String
)

data class AddDreamResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("dream_id")
    val dreamId: Int?,
    
    @SerializedName("message")
    val message: String
)

data class UpdateDreamRequest(
    @SerializedName("dream_id")
    val dreamId: Int,
    
    @SerializedName("dream_keyword")
    val dreamKeyword: String,
    
    @SerializedName("dream_interpretation")
    val dreamInterpretation: String,
    
    @SerializedName("lucky_numbers")
    val luckyNumbers: String,

    @SerializedName("category")
    val category: String
)

data class DreamAdminItem(
    @SerializedName("dream_id")
    val dreamId: Int,
    
    @SerializedName("dream_keyword")
    val dreamKeyword: String,
    
    @SerializedName("dream_interpretation")
    val dreamInterpretation: String,
    
    @SerializedName("lucky_numbers")
    val luckyNumbers: String,
    
    @SerializedName("view_count")
    val viewCount: Int,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("has_vector")
    val hasVector: Boolean,

    @SerializedName("category")
    val category: String? = "ความฝัน"
)
