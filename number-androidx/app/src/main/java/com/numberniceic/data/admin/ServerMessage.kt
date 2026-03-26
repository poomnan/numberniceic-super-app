package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class ServerMessage(
    @SerializedName("message") val message: String?, 
    @SerializedName("activity") val activity: String?,
    @SerializedName("debug") val debug: Any? = null
) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readValue(null)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(message)
        writeString(activity)
        writeValue(debug)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ServerMessage> = object : Parcelable.Creator<ServerMessage> {
            override fun createFromParcel(source: Parcel): ServerMessage = ServerMessage(source)
            override fun newArray(size: Int): Array<ServerMessage?> = arrayOfNulls(size)
        }
    }
}