package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Article(
    @SerializedName("art_id") val artId: Int,
    @SerializedName("slug") val slug: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("excerpt") val excerpt: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("is_published") val isPublished: Int,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("title_short") val titleShort: String?,
    @SerializedName("image_url") val imageUrl: String?
) : Parcelable {
    

    constructor(source: Parcel) : this(
        source.readInt(),
        source.readString(),
        source.readString(),
        source.readString(),
        source.readString(),
        source.readString(),
        source.readInt(),
        source.readString(),
        source.readString(),
        source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(artId)
        writeString(slug)
        writeString(title)
        writeString(excerpt)
        writeString(category)
        writeString(content)
        writeInt(isPublished)
        writeString(publishedAt)
        writeString(titleShort)
        writeString(imageUrl)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Article> = object : Parcelable.Creator<Article> {
            override fun createFromParcel(source: Parcel): Article = Article(source)
            override fun newArray(size: Int): Array<Article?> = arrayOfNulls(size)
        }
    }
}
