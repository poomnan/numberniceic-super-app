package com.numberniceic.data.home

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class ContinueDR(@SerializedName("conA") val conA: Int, @SerializedName("conB") val conB: Int) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(conA)
        writeInt(conB)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ContinueDR> = object : Parcelable.Creator<ContinueDR> {
            override fun createFromParcel(source: Parcel): ContinueDR = ContinueDR(source)
            override fun newArray(size: Int): Array<ContinueDR?> = arrayOfNulls(size)
        }
    }
}