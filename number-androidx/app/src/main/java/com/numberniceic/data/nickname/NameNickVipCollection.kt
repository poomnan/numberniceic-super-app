package com.numberniceic.data.nickname

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class NameNickVipCollection(@SerializedName("day") val day: String?, @SerializedName("nSingle") val nSingle: String?, @SerializedName("nick_name_list_vip") val NickNameListVip: ArrayList<NameNickVipItem>?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.createTypedArrayList(NameNickVipItem.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(day)
        writeString(nSingle)
        writeTypedList(NickNameListVip)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<NameNickVipCollection> = object : Parcelable.Creator<NameNickVipCollection> {
            override fun createFromParcel(source: Parcel): NameNickVipCollection = NameNickVipCollection(source)
            override fun newArray(size: Int): Array<NameNickVipCollection?> = arrayOfNulls(size)
        }
    }
}