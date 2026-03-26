package com.numberniceic.data.member

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class BagColorDaoCollection(@SerializedName("activity") val activity: String?, @SerializedName("member_bagcolor") val bagColor: List<BagColor>?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.createTypedArrayList(BagColor.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(activity)
        writeTypedList(bagColor)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<BagColorDaoCollection> = object : Parcelable.Creator<BagColorDaoCollection> {
            override fun createFromParcel(source: Parcel): BagColorDaoCollection = BagColorDaoCollection(source)
            override fun newArray(size: Int): Array<BagColorDaoCollection?> = arrayOfNulls(size)
        }
    }
}