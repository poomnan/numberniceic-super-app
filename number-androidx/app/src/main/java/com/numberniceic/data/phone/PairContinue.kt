package com.numberniceic.data.phone

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class PairContinue(@SerializedName("pairContinueD") val pairContinueD: Int, @SerializedName("pairContinueR") val pairContinueR: Int) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(pairContinueD)
        writeInt(pairContinueR)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PairContinue> = object : Parcelable.Creator<PairContinue> {
            override fun createFromParcel(source: Parcel): PairContinue = PairContinue(source)
            override fun newArray(size: Int): Array<PairContinue?> = arrayOfNulls(size)
        }
    }
}