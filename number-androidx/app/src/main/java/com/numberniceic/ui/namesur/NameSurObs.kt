package com.numberniceic.ui.namesur

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.numberniceic.BR

class NameSurObs: BaseObservable() {
    var birthDay: String = "เลือกวันเกิด"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.birthDay)
        }

    var name: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.name)
        }

    var surname: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.surname)
        }

    var satName: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.satName)
        }

    var satSurName: String = "?"
    @Bindable get
    set(value){
        field = value
        notifyPropertyChanged(BR.satSurName)
    }

    var kName: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.kName)
        }

    var pairSumSatNameSurname: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSumSatNameSurname)
        }


    var pairSumShaName: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSumShaName)
        }

    var ayantanaName: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.ayantanaName)
        }

    var ayantanaSurName: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.ayantanaSurName)
        }

    var ayantanaNameSurname: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.ayantanaNameSurname)
        }

    var pairSumShaSurname: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSumShaSurname)
        }

    var pairSumShaNameSurname: String = "?"
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSumShaNameSurname)
        }

}