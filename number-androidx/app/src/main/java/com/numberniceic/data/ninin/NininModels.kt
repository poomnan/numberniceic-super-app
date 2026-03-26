package com.numberniceic.data.ninin

import com.google.gson.annotations.SerializedName

data class NininChatRequest(
    @SerializedName("message")
    val message: String,
    @SerializedName("guest_id")
    val guestId: String? = null,
    @SerializedName("member_id")
    val memberId: String? = null
)

data class NininRedeemAccessRequest(
    @SerializedName("member_id")
    val memberId: String? = null,
    @SerializedName("guest_id")
    val guestId: String? = null,
    @SerializedName("package_code")
    val packageCode: String,
    @SerializedName("secret_code")
    val secretCode: String
)

data class NininRedeemAccessResponse(
    @SerializedName("ok")
    val ok: Boolean = false,
    @SerializedName("ref_no")
    val refNo: String? = null,
    @SerializedName("product_id")
    val productId: Int? = null,
    @SerializedName("package_code")
    val packageCode: String? = null,
    @SerializedName("source")
    val source: String? = null
)

data class NininChatResponse(
    @SerializedName("reply")
    val reply: String,
    
    @SerializedName("intent")
    val intent: String?, // DREAM, NAME, OTHER

    @SerializedName("dream_found")
    val dreamFound: Boolean,
    
    @SerializedName("dream_data")
    val dreamData: NininDreamData?,

    @SerializedName("name_found")
    val nameFound: Boolean = false,

    @SerializedName("name_data")
    val nameData: List<RecommendResult>?,
    
    @SerializedName("ninin_persona")
    val nininPersona: NininPersona?,
    
    @SerializedName("show_consult_button")
    val showConsultButton: Boolean = false,

    @SerializedName("show_packages")
    val showPackages: Boolean = false,

    @SerializedName("usage_count")
    val usageCount: Int? = null,

    @SerializedName("free_limit")
    val freeLimit: Int? = null,

    @SerializedName("free_remaining")
    val freeRemaining: Int? = null
)

data class RecommendResult(
    @SerializedName("name_id")
    val nameId: Int,
    @SerializedName("th_name")
    val thName: String,
    @SerializedName("distance")
    val distance: Double?,
    @SerializedName("sat_sum")
    val satSum: Int,
    @SerializedName("sha_sum")
    val shaSum: Int,
    @SerializedName("is_good_sat")
    val isGoodSat: Boolean,
    @SerializedName("is_good_sha")
    val isGoodSha: Boolean,
    @SerializedName("name_sat_sum")
    val nameSatSum: Int,
    @SerializedName("name_sha_sum")
    val nameShaSum: Int
)

data class NininDreamData(
    @SerializedName("keyword")
    val keyword: String,
    
    @SerializedName("interpretation")
    val interpretation: String,
    
    @SerializedName("lucky_numbers")
    val luckyNumbers: String
)

data class NininPersona(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("avatar_url")
    val avatarUrl: String
)
