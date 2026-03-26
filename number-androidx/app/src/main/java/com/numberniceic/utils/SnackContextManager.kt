package com.numberniceic.utils

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.numberniceic.R


class SnackContextManager {


    companion object {
        fun setSnack(messagex: String, coordinatorLayout: CoordinatorLayout) {
            val snack = Snackbar.make(coordinatorLayout, messagex, Snackbar.LENGTH_LONG)
            val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            tv.setTextColor(Color.YELLOW)
            snack.show()
        }

    }


}