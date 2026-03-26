package com.numberniceic.data.member

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class BagColor(@SerializedName("memberid") val memberId: Int, @SerializedName("age") val age: Int, @SerializedName("bag_desc") val bagDesc: String?, @SerializedName("bag_color1") val bagColor1: String?, @SerializedName("bag_color2") val bagColor2: String?, @SerializedName("bag_color3") val bagColor3: String?, @SerializedName("bag_color4") val bagColor4: String?, @SerializedName("bag_color5") val bagColor5: String?, @SerializedName("bag_color6") val bagColor6: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt(),
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
        writeInt(memberId)
        writeInt(age)
        writeString(bagDesc)
        writeString(bagColor1)
        writeString(bagColor2)
        writeString(bagColor3)
        writeString(bagColor4)
        writeString(bagColor5)
        writeString(bagColor6)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<BagColor> = object : Parcelable.Creator<BagColor> {
            override fun createFromParcel(source: Parcel): BagColor = BagColor(source)
            override fun newArray(size: Int): Array<BagColor?> = arrayOfNulls(size)
        }
    }
}