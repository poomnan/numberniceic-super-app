package com.numberniceic.data.phone

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class PairSpecialX(@SerializedName("countPairZero") val countPairZero: Int, @SerializedName("scoreDupMi") val scoreDupMi: Int, @SerializedName("percentTotalOfTotal") val percentTotalOfTotal: PercentTotalOfTotal?, @SerializedName("percentOriginD") val percentOriginD: Int, @SerializedName("percentOriginR") val percentOriginR: Int) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt(),
            source.readParcelable<PercentTotalOfTotal>(PercentTotalOfTotal::class.java.classLoader),
            source.readInt(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(countPairZero)
        writeInt(scoreDupMi)
        writeParcelable(percentTotalOfTotal, 0)
        writeInt(percentOriginD)
        writeInt(percentOriginR)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PairSpecialX> = object : Parcelable.Creator<PairSpecialX> {
            override fun createFromParcel(source: Parcel): PairSpecialX = PairSpecialX(source)
            override fun newArray(size: Int): Array<PairSpecialX?> = arrayOfNulls(size)
        }
    }
}