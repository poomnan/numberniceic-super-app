package com.numberniceic.data.persons

import com.google.gson.annotations.SerializedName

data class OutfitDayPalettesResponse(
    @SerializedName("day_palettes") val dayPalettes: Map<String, List<String>>?
)

data class OutfitDayPalettesUpsertRequest(
    @SerializedName("day_number") val dayNumber: Int? = null,
    @SerializedName("shades") val shades: List<String>? = null,
    @SerializedName("day_palettes") val dayPalettes: Map<String, List<String>>? = null
)
