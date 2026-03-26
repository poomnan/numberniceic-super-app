package com.numberniceic.data.home

import android.os.Parcel
import android.os.Parcelable

data class ScoreRD(val scoreD: Int, val scoreR: Int) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(scoreD)
        writeInt(scoreR)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ScoreRD> = object : Parcelable.Creator<ScoreRD> {
            override fun createFromParcel(source: Parcel): ScoreRD = ScoreRD(source)
            override fun newArray(size: Int): Array<ScoreRD?> = arrayOfNulls(size)
        }
    }
}