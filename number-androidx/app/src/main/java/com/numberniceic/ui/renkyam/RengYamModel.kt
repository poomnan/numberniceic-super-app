package com.numberniceic.ui.renkyam

import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import com.numberniceic.R

class RengYamModel: ViewModel() {
    val rengYamObs =  RengYamObs()

    init {
        this.initUI()
    }

    private fun initUI() {
        rengYamObs.txtCurrentDay = "วันจันทร์"

    }
}