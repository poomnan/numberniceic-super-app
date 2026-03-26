package com.numberniceic.data.persons

import com.google.gson.annotations.SerializedName

data class OutfitMiracleColorSetsRequest(
    @SerializedName("current_day_name") val currentDayName: String,
    @SerializedName("birth_day_name") val birthDayName: String,
    @SerializedName("age_years") val ageYears: Int
)
