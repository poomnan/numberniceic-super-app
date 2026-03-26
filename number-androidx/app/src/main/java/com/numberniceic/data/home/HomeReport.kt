package com.numberniceic.data.home

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class HomeReport(@SerializedName("miracleD") val miracleD: String?, @SerializedName("miracleR") val miracleR: String?) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(miracleD)
        writeString(miracleR)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<HomeReport> = object : Parcelable.Creator<HomeReport> {
            override fun createFromParcel(source: Parcel): HomeReport = HomeReport(source)
            override fun newArray(size: Int): Array<HomeReport?> = arrayOfNulls(size)
        }
    }
}