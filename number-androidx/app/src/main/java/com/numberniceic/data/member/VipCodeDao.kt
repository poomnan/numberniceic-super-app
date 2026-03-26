package com.numberniceic.data.member

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.numberniceic.data.apicollectiondao.HomeCollectionDao

data class VipCodeDao(
        @SerializedName("vipid") val vipid: Int,
        @SerializedName("vipcode") val vipcode: String?,
        @SerializedName("userdetial") val userdetial: String?,
        @SerializedName("viptype") val viptype: String?,
        @SerializedName("vipstatus") val vipstatus: String?


) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readInt(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(vipid)
        writeString(vipcode)
        writeString(userdetial)
        writeString(viptype)
        writeString(vipstatus)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<VipCodeDao> = object : Parcelable.Creator<VipCodeDao> {
            override fun createFromParcel(source: Parcel): VipCodeDao = VipCodeDao(source)
            override fun newArray(size: Int): Array<VipCodeDao?> = arrayOfNulls(size)
        }
    }
}