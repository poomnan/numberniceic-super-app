package com.numberniceic.data.news

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class News(@SerializedName("newsid") val newsId: String?, @SerializedName("news_pic_header") val newsImg: String?, @SerializedName("news_headline") val newsHeader: String?, @SerializedName("news_desc") val newsDesc: String?, @SerializedName("news_detail") val newsDetail: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(newsId)
        writeString(newsImg)
        writeString(newsHeader)
        writeString(newsDesc)
        writeString(newsDetail)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<News> = object : Parcelable.Creator<News> {
            override fun createFromParcel(source: Parcel): News = News(source)
            override fun newArray(size: Int): Array<News?> = arrayOfNulls(size)
        }
    }
}