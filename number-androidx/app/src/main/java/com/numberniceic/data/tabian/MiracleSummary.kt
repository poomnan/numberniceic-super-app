package com.numberniceic.data.tabian

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

class MiracleSummary(@SerializedName("miracleD") val miracleD: String?, @SerializedName("miracleR") val miracleR: String?) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(miracleD)
        writeString(miracleR)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MiracleSummary> = object : Parcelable.Creator<MiracleSummary> {
            override fun createFromParcel(source: Parcel): MiracleSummary = MiracleSummary(source)
            override fun newArray(size: Int): Array<MiracleSummary?> = arrayOfNulls(size)
        }
    }
}