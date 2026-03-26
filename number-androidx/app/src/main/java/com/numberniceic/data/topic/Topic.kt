package com.numberniceic.data.topic

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Topic(@SerializedName("topic_id") val topicId: String?, @SerializedName("topic_date") val topicDate: String?, @SerializedName("auth_name") val authName: String?, @SerializedName("header_text") val headerText: String?, @SerializedName("desc_text") val descText: String?, @SerializedName("hag_phone") val tagPhone: String?, @SerializedName("hag_tabian") val tagTabian: String?, @SerializedName("hag_home") val tagHome: String?, @SerializedName("hag_namesur") val tagNameSur: String?, @SerializedName("paragraph1_text") val paragraph1Text: String?, @SerializedName("paragraph2_text") val paragraph2Text: String?, @SerializedName("paragraph3_text") val paragraph3Text: String?, @SerializedName("photo1") val photo1: String?, @SerializedName("photo2") val photo2: String?, @SerializedName("photo3") val photo3: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(topicId)
        writeString(topicDate)
        writeString(authName)
        writeString(headerText)
        writeString(descText)
        writeString(tagPhone)
        writeString(tagTabian)
        writeString(tagHome)
        writeString(tagNameSur)
        writeString(paragraph1Text)
        writeString(paragraph2Text)
        writeString(paragraph3Text)
        writeString(photo1)
        writeString(photo2)
        writeString(photo3)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Topic> = object : Parcelable.Creator<Topic> {
            override fun createFromParcel(source: Parcel): Topic = Topic(source)
            override fun newArray(size: Int): Array<Topic?> = arrayOfNulls(size)
        }
    }
}