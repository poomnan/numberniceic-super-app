package com.numberniceic.data.member

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class MemberVipCollectionDao(@SerializedName("member") val member: String?, @SerializedName("vipcode") val vipcode: VipCodeDao?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readParcelable<VipCodeDao>(VipCodeDao::class.java.classLoader)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(member)
        writeParcelable(vipcode, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MemberVipCollectionDao> = object : Parcelable.Creator<MemberVipCollectionDao> {
            override fun createFromParcel(source: Parcel): MemberVipCollectionDao = MemberVipCollectionDao(source)
            override fun newArray(size: Int): Array<MemberVipCollectionDao?> = arrayOfNulls(size)
        }
    }
}