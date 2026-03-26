package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Serverx(
    @SerializedName("serverx") val serverx: ServerMessage?,
    @SerializedName("userx") val userx: Userx?,
    @SerializedName("rengyam_access") val rengyamAccess: RengyamAccess? = null
) : Parcelable {

    constructor(source: Parcel) : this(
            source.readParcelable<ServerMessage>(ServerMessage::class.java.classLoader),
            source.readParcelable<Userx>(Userx::class.java.classLoader),
            source.readParcelable<RengyamAccess>(RengyamAccess::class.java.classLoader)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(serverx, 0)
        writeParcelable(userx, 0)
        writeParcelable(rengyamAccess, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Serverx> = object : Parcelable.Creator<Serverx> {
            override fun createFromParcel(source: Parcel): Serverx = Serverx(source)
            override fun newArray(size: Int): Array<Serverx?> = arrayOfNulls(size)
        }
    }
}
