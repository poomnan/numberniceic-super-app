package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Colorx(@SerializedName("bag_id") val bagId: String?, @SerializedName("memberid") val memberId: String?, @SerializedName("age") val age: String?, @SerializedName("bag_color1") val bagColor1: String?, @SerializedName("bag_color2") val bagColor2: String?, @SerializedName("bag_color3") val bagColor3: String?, @SerializedName("bag_color4") val bagColor4: String?, @SerializedName("bag_color5") val bagColor5: String?, @SerializedName("bag_color6") val bagColor6: String?, @SerializedName("date_color_updated") val dateColorUpdated: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(bagId)
        writeString(memberId)
        writeString(age)
        writeString(bagColor1)
        writeString(bagColor2)
        writeString(bagColor3)
        writeString(bagColor4)
        writeString(bagColor5)
        writeString(bagColor6)
        writeString(dateColorUpdated)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Colorx> = object : Parcelable.Creator<Colorx> {
            override fun createFromParcel(source: Parcel): Colorx = Colorx(source)
            override fun newArray(size: Int): Array<Colorx?> = arrayOfNulls(size)
        }
    }
}