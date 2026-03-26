package com.numberniceic.data.news

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class NewsHeadlineCollection(@SerializedName("type_id") val typeId: String?, @SerializedName("news_all_type") val newsAll: List<NewsHeadline>?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.createTypedArrayList(NewsHeadline.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(typeId)
        writeTypedList(newsAll)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<NewsHeadlineCollection> = object : Parcelable.Creator<NewsHeadlineCollection> {
            override fun createFromParcel(source: Parcel): NewsHeadlineCollection = NewsHeadlineCollection(source)
            override fun newArray(size: Int): Array<NewsHeadlineCollection?> = arrayOfNulls(size)
        }
    }
}