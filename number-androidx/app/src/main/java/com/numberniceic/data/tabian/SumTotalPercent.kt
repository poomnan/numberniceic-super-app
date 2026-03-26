package com.numberniceic.data.tabian

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

class SumTotalPercent(@SerializedName("sumPercentD") val sumPercentD: Int, @SerializedName("sumPercentR") val sumPercentR: Int) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(sumPercentD)
        writeInt(sumPercentR)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SumTotalPercent> = object : Parcelable.Creator<SumTotalPercent> {
            override fun createFromParcel(source: Parcel): SumTotalPercent = SumTotalPercent(source)
            override fun newArray(size: Int): Array<SumTotalPercent?> = arrayOfNulls(size)
        }
    }
}