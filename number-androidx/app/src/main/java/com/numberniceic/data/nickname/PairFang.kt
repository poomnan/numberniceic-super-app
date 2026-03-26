package com.numberniceic.data.nickname

import android.os.Parcel
import android.os.Parcelable

data class PairFang(val pair: String?, val fang: String?) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(pair)
        writeString(fang)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PairFang> = object : Parcelable.Creator<PairFang> {
            override fun createFromParcel(source: Parcel): PairFang = PairFang(source)
            override fun newArray(size: Int): Array<PairFang?> = arrayOfNulls(size)
        }
    }
}