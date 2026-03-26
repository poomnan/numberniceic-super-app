package com.numberniceic.data.nickname

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.numberniceic.data.apicollectiondao.HomeCollectionDao

data class NameNickVipItem(@SerializedName("nameid") val nicnameid: String?, @SerializedName("day") val day: String?, @SerializedName("thainame") val thainame: String?, @SerializedName("reangthai") val reangthai: String?, @SerializedName("engname") val engname: String?,
                           @SerializedName("reangeng") val reangeng: String?, @SerializedName("leksat_thai") val leksat_thai: String?, @SerializedName("shadow") val shadow: String?, @SerializedName("leksat_eng") val leksat_eng: String?, @SerializedName("sex") val sex: String?) : Parcelable {
    

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
        writeString(nicnameid)
        writeString(day)
        writeString(thainame)
        writeString(reangthai)
        writeString(engname)
        writeString(reangeng)
        writeString(leksat_thai)
        writeString(shadow)
        writeString(leksat_eng)
        writeString(sex)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<NameNickVipItem> = object : Parcelable.Creator<NameNickVipItem> {
            override fun createFromParcel(source: Parcel): NameNickVipItem = NameNickVipItem(source)
            override fun newArray(size: Int): Array<NameNickVipItem?> = arrayOfNulls(size)
        }
    }
}