package com.numberniceic.ui.admin

import android.graphics.Color
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.numberniceic.BR
import com.numberniceic.R

class BagColorObs: BaseObservable() {

    var userDayOfWeek: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.userDayOfWeek)
        }

    var imgSave: Int = 1
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.imgSave)
        }


    var msgSave: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.msgSave)
        }

    var onSave: Boolean = true
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.onSave)
        }


    var nextYear: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.nextYear)
        }


    var currentYear: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.currentYear)
        }

    var userAge: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.userAge)
        }


    var userName: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.userName)
        }

    var userId: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.userId)
        }

    var colorChip0: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChip0)
        }

    var colorChip1: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChip1)
        }

    var colorChip2: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChip2)
        }

    var colorChip3: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChip3)
        }

    var colorChip4: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChip4)
        }

    var colorChip5: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChip5)
        }


    var colorChipb0: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChipb0)
        }

    var colorChipb1: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChipb1)
        }

    var colorChipb2: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChipb2)
        }

    var colorChipb3: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChipb3)
        }

    var colorChipb4: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChipb4)
        }

    var colorChipb5: Int = Color.parseColor("#ffffff")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.colorChipb5)
        }


    var lastUpdateA: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.lastUpdateA)
        }

    var lastUpdateB: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.lastUpdateB)
        }
}