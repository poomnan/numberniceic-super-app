package com.numberniceic.data.persons

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class CusTime(@SerializedName("current_time") val currentTime: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(currentTime)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<CusTime> = object : Parcelable.Creator<CusTime> {
            override fun createFromParcel(source: Parcel): CusTime = CusTime(source)
            override fun newArray(size: Int): Array<CusTime?> = arrayOfNulls(size)
        }
    }
}