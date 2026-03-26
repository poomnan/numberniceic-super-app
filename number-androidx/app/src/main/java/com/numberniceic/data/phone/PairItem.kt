package com.numberniceic.data.phone

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class PairItem(@SerializedName("pair") val pair: String?, @SerializedName("type") val type: String?, @SerializedName("point") val point: Int, @SerializedName("bonusScore") val bonusScore: Int) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(pair)
        writeString(type)
        writeInt(point)
        writeInt(bonusScore)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PairItem> = object : Parcelable.Creator<PairItem> {
            override fun createFromParcel(source: Parcel): PairItem = PairItem(source)
            override fun newArray(size: Int): Array<PairItem?> = arrayOfNulls(size)
        }
    }
}