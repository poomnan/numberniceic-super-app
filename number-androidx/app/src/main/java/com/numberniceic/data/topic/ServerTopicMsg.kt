package com.numberniceic.data.topic

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ServerTopicMsg(@SerializedName("topic_message") val topicMsg: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(topicMsg)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ServerTopicMsg> = object : Parcelable.Creator<ServerTopicMsg> {
            override fun createFromParcel(source: Parcel): ServerTopicMsg = ServerTopicMsg(source)
            override fun newArray(size: Int): Array<ServerTopicMsg?> = arrayOfNulls(size)
        }
    }
}