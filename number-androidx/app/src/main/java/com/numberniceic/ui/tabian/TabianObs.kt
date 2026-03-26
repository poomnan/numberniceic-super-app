package com.numberniceic.ui.tabian

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.numberniceic.BR
import com.numberniceic.R

class TabianObs : BaseObservable() {


    var vip: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.vip)
        }

    var edtTabian: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.edtTabian)
        }

    var edtTabianMud: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.edtTabianMud)
        }

    var miracleD: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.miracleD)
        }

    var miracleR: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.miracleR)
        }


    var sumTotalPercentD: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.sumTotalPercentD)
        }

    var sumTotalPercentR: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.sumTotalPercentR)
        }

    var txtNumMud: String = "0"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtNumMud)
        }


    var acsonMud: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.acsonMud)
        }


    var pleNumMud: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pleNumMud)
        }

    var pleNumMudColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pleNumMudColor)
        }

    var plepudStrMud: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.plepudStrMud)
        }

    var plepudStrMudColor: Int = 9
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.plepudStrMudColor)
        }


    var tabianScoreD: String = "0"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.tabianScoreD)
        }

    var tabianScoreR: String = "0"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.tabianScoreR)
        }

    var pairSumAll: String = "0"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSumAll)
        }


    var pairSumSecond: String = "0"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSumSecond)
        }

    var bgPairAll: Int = 9
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.bgPairAll)
        }

    var bgPairSecond: Int = 2
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.bgPairSecond)
        }

    var txtNumReang: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtNumReang)
        }

    var imgNumReangMud: Int = 9
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.imgNumReangMud)
        }

    var txtNumRoam: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtNumRoam)
        }

    var txtNumSecond: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtNumSecond)
        }

    var txtConA: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtConA)
        }

    var txtConB: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtConB)
        }
}