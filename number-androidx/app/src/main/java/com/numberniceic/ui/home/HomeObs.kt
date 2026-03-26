package com.numberniceic.ui.home

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.numberniceic.BR

class HomeObs: BaseObservable() {


    var miracleD: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.miracleD)
        }

    var miracleR: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.miracleR)
        }

    var homeId: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.homeId)
        }

    var scoreImg: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.scoreImg)
        }

    var scoreD: String = "0"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.scoreD)
        }

    var scoreR: String = "0"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.scoreR)
        }

    var pairSumNumber: String = "0"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSumNumber)
        }

    var bgPairSumNumber: Int = 2
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.bgPairSumNumber)
        }

    var pairGradeSum: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairGradeSum)
        }

    var percentPairsA: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.percentPairsA)
        }

    var percentPairsB: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.percentPairsB)
        }

    var gradeNumReang: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.gradeNumReang)
        }






}