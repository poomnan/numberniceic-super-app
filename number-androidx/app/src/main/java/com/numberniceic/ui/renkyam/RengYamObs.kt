package com.numberniceic.ui.renkyam

import android.widget.ImageView
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.numberniceic.BR
import com.numberniceic.R

import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.ViewModel

class RengYamObs: ViewModel(), Observable {
    private val callbacks: PropertyChangeRegistry = PropertyChangeRegistry()

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        callbacks.add(callback)
    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        callbacks.remove(callback)
    }

    fun notifyPropertyChanged(fieldId: Int) {
        callbacks.notifyCallbacks(this, fieldId, null)
    }

    var txtCurrentDay: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtCurrentDay)
        }


}

