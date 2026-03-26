package com.numberniceic.ui.namenick

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.numberniceic.BR

class NameNickObs: BaseObservable() {




    var bgColorShaSum: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.bgColorShaSum)
        }

    var bgColorSatSum: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.bgColorSatSum)
        }

    var gradeSatNickname: String = "เกรด"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.gradeSatNickname)
        }
    var gradeShaNickname: String = "เกรด"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.gradeShaNickname)
        }

    var birthDay: String = "เลือกวันเกิด"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.birthDay)
        }

    var pairSatNickName: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSatNickName)
        }

    var pairShaNickName: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairShaNickName)
        }

    var satNickName: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.satNickName)
        }


    var kName: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.kName)
        }
}