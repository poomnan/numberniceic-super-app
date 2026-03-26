package com.numberniceic.data.tabian

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

class PairsMiracle(@SerializedName("pairnumber") val pairnumber: String?, @SerializedName("percent") val percent: Int, @SerializedName("pairtype") val pairtype: String?, @SerializedName("pairpoint") val pairpoint: Int, @SerializedName("miracledesc") val miracledesc: String?, @SerializedName("miracledetail") val miracledetail: String?) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readInt(),
            source.readString(),
            source.readInt(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(pairnumber)
        writeInt(percent)
        writeString(pairtype)
        writeInt(pairpoint)
        writeString(miracledesc)
        writeString(miracledetail)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PairsMiracle> = object : Parcelable.Creator<PairsMiracle> {
            override fun createFromParcel(source: Parcel): PairsMiracle = PairsMiracle(source)
            override fun newArray(size: Int): Array<PairsMiracle?> = arrayOfNulls(size)
        }
    }
}