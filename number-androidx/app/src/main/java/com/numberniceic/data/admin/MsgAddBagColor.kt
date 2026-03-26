package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class MsgAddBagColor(@SerializedName("insert_color") val insertColor: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(insertColor)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MsgAddBagColor> = object : Parcelable.Creator<MsgAddBagColor> {
            override fun createFromParcel(source: Parcel): MsgAddBagColor = MsgAddBagColor(source)
            override fun newArray(size: Int): Array<MsgAddBagColor?> = arrayOfNulls(size)
        }
    }
}