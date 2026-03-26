package com.numberniceic.data.nickname

import android.os.Parcel
import android.os.Parcelable

data class CharNum(val xChar: String?, val xNum: String?) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(xChar)
        writeString(xNum)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<CharNum> = object : Parcelable.Creator<CharNum> {
            override fun createFromParcel(source: Parcel): CharNum = CharNum(source)
            override fun newArray(size: Int): Array<CharNum?> = arrayOfNulls(size)
        }
    }
}