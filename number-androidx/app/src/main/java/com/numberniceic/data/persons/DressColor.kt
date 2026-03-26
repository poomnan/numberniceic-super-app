package com.numberniceic.data.persons
import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class DressColor(@SerializedName("colorid") val colorId: String?, @SerializedName("day_eng") val dayEng: String?, @SerializedName("color_code1") val colorCode1: String?, @SerializedName("color_code2") val colorCode2: String?, @SerializedName("color_code3") val colorCode3: String?, @SerializedName("color_code4") val colorCode4: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(colorId)
        writeString(dayEng)
        writeString(colorCode1)
        writeString(colorCode2)
        writeString(colorCode3)
        writeString(colorCode4)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<DressColor> = object : Parcelable.Creator<DressColor> {
            override fun createFromParcel(source: Parcel): DressColor = DressColor(source)
            override fun newArray(size: Int): Array<DressColor?> = arrayOfNulls(size)
        }
    }
}

