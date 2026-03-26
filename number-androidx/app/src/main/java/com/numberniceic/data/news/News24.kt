package com.numberniceic.data.news

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
data class News24(@SerializedName("news_hot") val newsHot: List<NewsHeadline>?, @SerializedName("news_feedback") val newsFeedback: List<NewsHeadline>?, @SerializedName("news_phonenum") val newsPhonenum: List<NewsHeadline>?, @SerializedName("news_namesur") val newsNameSur: List<NewsHeadline>?, @SerializedName("news_tabian") val newsTabian: List<NewsHeadline>?, @SerializedName("news_homenum") val newsHome: List<NewsHeadline>?, @SerializedName("news_concept") val newsConcept: List<NewsHeadline>?) : Parcelable {

    constructor(source: Parcel) : this(
            source.createTypedArrayList(NewsHeadline.CREATOR),
            source.createTypedArrayList(NewsHeadline.CREATOR),
            source.createTypedArrayList(NewsHeadline.CREATOR),
            source.createTypedArrayList(NewsHeadline.CREATOR),
            source.createTypedArrayList(NewsHeadline.CREATOR),
            source.createTypedArrayList(NewsHeadline.CREATOR),
            source.createTypedArrayList(NewsHeadline.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeTypedList(newsHot)
        writeTypedList(newsFeedback)
        writeTypedList(newsPhonenum)
        writeTypedList(newsNameSur)
        writeTypedList(newsTabian)
        writeTypedList(newsHome)
        writeTypedList(newsConcept)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<News24> = object : Parcelable.Creator<News24> {
            override fun createFromParcel(source: Parcel): News24 = News24(source)
            override fun newArray(size: Int): Array<News24?> = arrayOfNulls(size)
        }
    }
}