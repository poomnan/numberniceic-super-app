package com.numberniceic.data.phone

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class PercentTotalOfTotal(@SerializedName("percentTotalD") val percentTotalD: Int, @SerializedName("percentTotalR") val percentTotalR: Int) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(percentTotalD)
        writeInt(percentTotalR)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PercentTotalOfTotal> = object : Parcelable.Creator<PercentTotalOfTotal> {
            override fun createFromParcel(source: Parcel): PercentTotalOfTotal = PercentTotalOfTotal(source)
            override fun newArray(size: Int): Array<PercentTotalOfTotal?> = arrayOfNulls(size)
        }
    }
}