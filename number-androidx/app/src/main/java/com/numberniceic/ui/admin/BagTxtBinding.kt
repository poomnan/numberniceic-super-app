package com.numberniceic.ui.admin

import android.widget.TextView
import androidx.annotation.NonNull
import androidx.databinding.BindingAdapter

class BagTxtBinding {
    companion object {
        @BindingAdapter("android:text")
        @JvmStatic
        fun setIntToText(@NonNull textView: TextView, @NonNull anInt: Int) {
            textView.text = anInt.toString()
        }
    }
}
