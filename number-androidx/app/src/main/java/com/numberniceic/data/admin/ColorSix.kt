package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ColorSix(@SerializedName("user_id") val userId: String?, @SerializedName("color_six_a") val colorSixA: Colorx?, @SerializedName("color_six_b") val colorSixB: Colorx?) : Parcelable {

    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readParcelable<Colorx>(Colorx::class.java.classLoader),
            source.readParcelable<Colorx>(Colorx::class.java.classLoader)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(userId)
        writeParcelable(colorSixA, 0)
        writeParcelable(colorSixB, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ColorSix> = object : Parcelable.Creator<ColorSix> {
            override fun createFromParcel(source: Parcel): ColorSix = ColorSix(source)
            override fun newArray(size: Int): Array<ColorSix?> = arrayOfNulls(size)
        }
    }
}