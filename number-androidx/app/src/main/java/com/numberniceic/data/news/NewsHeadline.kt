package com.numberniceic.data.news

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class NewsHeadline(@SerializedName("newsid", alternate = ["news_id", "id"]) val newsId: String?, @SerializedName("fix") val fix: String?, @SerializedName("news_pic_header") val newsImg: String?, @SerializedName("news_headline") val newsHeader: String?, @SerializedName("news_title_short") val newsTitleShort: String?, @SerializedName("news_desc") val newsDesc: String?, @SerializedName("news_detail") val newsDetail: String?, @SerializedName("category") val category: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
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
        writeString(newsId)
        writeString(fix)
        writeString(newsImg)
        writeString(newsHeader)
        writeString(newsTitleShort)
        writeString(newsDesc)
        writeString(newsDetail)
        writeString(category)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<NewsHeadline> = object : Parcelable.Creator<NewsHeadline> {
            override fun createFromParcel(source: Parcel): NewsHeadline = NewsHeadline(source)
            override fun newArray(size: Int): Array<NewsHeadline?> = arrayOfNulls(size)
        }
    }
}