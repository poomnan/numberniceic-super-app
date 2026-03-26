package com.numberniceic.ui.apersonnews

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.numberniceic.BR

class ClothColor3dObs: BaseObservable() {

    var clothColorTitle: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.clothColorTitle)
        }

    var clothColorTitleX1: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.clothColorTitleX1)
        }

    var clothColorTitleX2: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.clothColorTitleX2)
        }




}