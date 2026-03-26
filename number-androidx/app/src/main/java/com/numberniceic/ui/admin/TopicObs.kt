package com.numberniceic.ui.admin

import android.net.Uri
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.numberniceic.BR


class TopicObs:BaseObservable() {



    var topicId: String? = null
        @Bindable get
        set(value) {
            field = value
            notifyPropertyChanged(BR.topicId)
        }

    var topicDateTime: String? = null
        @Bindable get
        set(value) {
            field = value
            notifyPropertyChanged(BR.topicDateTime)
        }

    var authName: String? = null
        @Bindable get
        set(value) {
            field = value
            notifyPropertyChanged(BR.authName)
        }

    var imgBase64Photo3: String? = null
        @Bindable get
        set(value) {
            field = value
            notifyPropertyChanged(BR.imgBase64Photo3)
        }

    var imgBase64Photo2: String? = null
        @Bindable get
        set(value) {
            field = value
            notifyPropertyChanged(BR.imgBase64Photo2)
        }

    var imgBase64Photo1: String? = null
        @Bindable get
        set(value) {
            field = value
            notifyPropertyChanged(BR.imgBase64Photo1)
        }

    var chipPhone: Boolean = false
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.chipPhone)
        }

    var chipTabian: Boolean = false
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.chipTabian)
        }

    var chipHome: Boolean = false
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.chipHome)
        }

    var chipNameSur: Boolean = false
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.chipNameSur)
        }


    var imgParagraph1: Uri? = null
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.imgParagraph1)
        }
    var imgParagraph2: Uri? = null
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.imgParagraph2)
        }

    var imgHeader: Uri? = null
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.imgHeader)
        }

    var topicHeader: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.topicHeader)
        }

    var topicParagraph1: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.topicParagraph1)
        }

    var topicParagraph2: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.topicParagraph2)
        }

    var topicParagraph3: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.topicParagraph3)
        }

    var topicDesc: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.topicDesc)
        }
}