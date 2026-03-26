package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class ServerVip(@SerializedName("viplevel") val viplevel: String?, @SerializedName("message") val message: String?, @SerializedName("codename") val codename: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(viplevel)
        writeString(message)
        writeString(codename)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ServerVip> = object : Parcelable.Creator<ServerVip> {
            override fun createFromParcel(source: Parcel): ServerVip = ServerVip(source)
            override fun newArray(size: Int): Array<ServerVip?> = arrayOfNulls(size)
        }
    }
}