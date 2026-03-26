package com.numberniceic.data.persons

import com.google.gson.annotations.SerializedName

data class OutfitMiracleColorSetItem(
    @SerializedName("label") val label: String?,
    @SerializedName("source_day") val sourceDay: String?,
    @SerializedName("source_age") val sourceAge: Int?,
    @SerializedName("number") val number: Int?,
    @SerializedName("day") val day: String?,
    @SerializedName("dual_numbers") val dualNumbers: List<Int>?,
    @SerializedName("dual_days") val dualDays: List<String>?,
    @SerializedName("color_group") val colorGroup: List<String>?
)

data class OutfitMiracleColorSetsResponse(
    @SerializedName("current_day") val currentDay: String?,
    @SerializedName("birth_day") val birthDay: String?,
    @SerializedName("age") val age: Int?,
    @SerializedName("reference_project") val referenceProject: String?,
    @SerializedName("reference_skill_doc") val referenceSkillDoc: String?,
    @SerializedName("reference_code_file") val referenceCodeFile: String?,
    @SerializedName("auspicious_sets") val auspiciousSets: List<OutfitMiracleColorSetItem>?,
    @SerializedName("inauspicious_sets") val inauspiciousSets: List<OutfitMiracleColorSetItem>?
)
