package com.numberniceic.ui.admin

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.numberniceic.R

class TopicDatabinding {
    companion object {

        @BindingAdapter("app:chipBackgroundColor")
        @JvmStatic
        fun setBgChipTag(@NonNull chip: Chip, @NonNull boolean: Boolean) {
            if (boolean) {
                chip.chipBackgroundColor = ContextCompat.getColorStateList(chip.context, R.color.colorChipHashTagClick)
            } else {
                chip.chipBackgroundColor = ContextCompat.getColorStateList(chip.context, R.color.colorBGWhite)
            }

        }

        @BindingAdapter("android:src")
        @JvmStatic
        fun setImageUri(@NonNull imgView: ImageView, @NonNull imgUri: Uri?) {
            if (imgUri == null) {
                imgView.setImageResource(R.drawable.image)
            } else {
                Glide.with(imgView.context)
                        .load(imgUri).into(imgView)
            }

        }

        @BindingAdapter("android:src")
        @JvmStatic
        fun setImageString(@NonNull imgView: ImageView, @NonNull imgStringUrl: String?) {
            if (imgStringUrl == null) {
                imgView.setImageResource(R.drawable.image)
            } else {
                Glide.with(imgView.context)
                        .load(imgStringUrl).into(imgView)
            }

        }

        @BindingAdapter("android:src")
        @JvmStatic
        fun setImageDrawable(@NonNull imgView: ImageView, @NonNull drawable: Drawable) {
            imgView.setImageDrawable(drawable)
        }

        @BindingAdapter("android:src")
        @JvmStatic
        fun setImageResource(@NonNull imgView: ImageView, @NonNull resource: Int) {
            imgView.setImageResource(resource)
        }

    }
}
