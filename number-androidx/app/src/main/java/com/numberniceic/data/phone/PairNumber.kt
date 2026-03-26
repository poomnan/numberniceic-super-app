package com.numberniceic.data.phone

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class PairNumber(
    @SerializedName("pairNumber") val pairNumber: String?,
    @SerializedName("pairType") val pairType: String?,
    @SerializedName("pairPoint") val pairPoint: Int?,
    @SerializedName("percentile") val percentile: Int?
) : Parcelable {

    constructor(source: Parcel) : this(
        source.readString(),
        source.readString(),
        source.readValue(Int::class.java.classLoader) as Int?,
        source.readValue(Int::class.java.classLoader) as Int?
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(pairNumber)
        writeString(pairType)
        writeValue(pairPoint)
        writeValue(percentile)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PairNumber> = object : Parcelable.Creator<PairNumber> {
            override fun createFromParcel(source: Parcel): PairNumber = PairNumber(source)
            override fun newArray(size: Int): Array<PairNumber?> = arrayOfNulls(size)
        }
    }
}
