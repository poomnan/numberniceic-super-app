package com.numberniceic.data.phone

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class ScoreTotal(@SerializedName("scoreTotalD") val scoreTotalD: Int, @SerializedName("scoreTotalR") val scoreTotalR: Int) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(scoreTotalD)
        writeInt(scoreTotalR)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ScoreTotal> = object : Parcelable.Creator<ScoreTotal> {
            override fun createFromParcel(source: Parcel): ScoreTotal = ScoreTotal(source)
            override fun newArray(size: Int): Array<ScoreTotal?> = arrayOfNulls(size)
        }
    }
}