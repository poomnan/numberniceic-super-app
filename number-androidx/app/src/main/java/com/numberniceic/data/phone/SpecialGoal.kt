package com.numberniceic.data.phone

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class SpecialGoal(@SerializedName("specialPairSum") val specialPairSum: String?, @SerializedName("specialPairLast") val specialPairLast: String?, @SerializedName("specialPairContinue") val specialPairContinue: String?) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(specialPairSum)
        writeString(specialPairLast)
        writeString(specialPairContinue)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SpecialGoal> = object : Parcelable.Creator<SpecialGoal> {
            override fun createFromParcel(source: Parcel): SpecialGoal = SpecialGoal(source)
            override fun newArray(size: Int): Array<SpecialGoal?> = arrayOfNulls(size)
        }
    }
}