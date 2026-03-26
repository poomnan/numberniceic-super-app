package com.numberniceic.data.rengyam

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class LuckyNumber(@SerializedName("lucky_date") val luckyDate: String?, @SerializedName("numbers") val number: String?, @SerializedName("active") val active: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(luckyDate)
        writeString(number)
        writeString(active)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<LuckyNumber> = object : Parcelable.Creator<LuckyNumber> {
            override fun createFromParcel(source: Parcel): LuckyNumber = LuckyNumber(source)
            override fun newArray(size: Int): Array<LuckyNumber?> = arrayOfNulls(size)
        }
    }
}