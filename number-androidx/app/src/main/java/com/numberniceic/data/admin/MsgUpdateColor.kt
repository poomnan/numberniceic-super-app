package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class MsgUpdateColor(@SerializedName("success_update_a") val successUpdateA: String?, @SerializedName("success_update_b") val successUpdateB: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(successUpdateA)
        writeString(successUpdateB)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MsgUpdateColor> = object : Parcelable.Creator<MsgUpdateColor> {
            override fun createFromParcel(source: Parcel): MsgUpdateColor = MsgUpdateColor(source)
            override fun newArray(size: Int): Array<MsgUpdateColor?> = arrayOfNulls(size)
        }
    }
}