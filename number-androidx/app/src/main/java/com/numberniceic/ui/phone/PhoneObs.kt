package com.numberniceic.ui.phone

import android.graphics.Color
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.numberniceic.BR

class PhoneObs : BaseObservable(){

    var edtPhone: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.edtPhone)
        }

    var reportSummaryD: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.reportSummaryD)
        }

    var reportSummaryR: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.reportSummaryR)
        }

    var imgReportR: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.imgReportR)
        }

    var imgReportD: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.imgReportD)
        }


    var txtPairSum: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtPairSum)
        }

    var txtPairLast: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtPairLast)
        }

    var txtPairCon: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.txtPairCon)
        }


    var gradePairsB: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.gradePairsB)
        }

    var gradePairsA: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.gradePairsA)
        }

    var gradePairSum: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.gradePairSum)
        }

    var scoreD: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.scoreD)
        }

    var scoreR: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.scoreR)
        }



    var percentD: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.percentD)
        }

    var percentR: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.percentR)
        }


    var pairAp1: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp1)
        }

    var pairAp2: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp2)
        }

    var pairAp3: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp3)
        }

    var pairAp4: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp4)
        }

    var pairAp5: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp5)
        }

    var pairBp1: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairBp1)
        }

    var pairBp2: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairBp2)
        }

    var pairBp3: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairBp3)
        }

    var pairBp4: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairBp4)
        }



    var pairSum: String = ""
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSum)
        }

    var pairSumBgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSumBgColor)
        }

    var pairAp1BgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp1BgColor)
        }

    var pairAp2BgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp2BgColor)
        }

    var pairAp3BgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp3BgColor)
        }

    var pairAp4BgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp4BgColor)
        }

    var pairAp5BgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp5BgColor)
        }

    var pairB1BgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairB1BgColor)
        }


    var pairB2BgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairB2BgColor)
        }


    var pairB3BgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairB3BgColor)
        }


    var pairB4BgColor: Int = 0
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairB4BgColor)
        }



    var pairAp1TxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp1TxtColor)
        }

    var pairAp2TxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp2TxtColor)
        }


    var pairAp3TxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp3TxtColor)
        }

    var pairAp4TxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp4TxtColor)
        }

    var pairAp5TxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairAp5TxtColor)
        }

    var pairBp1TxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairBp1TxtColor)
        }

    var pairBp2TxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairBp2TxtColor)
        }

    var pairBp3TxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairBp3TxtColor)
        }

    var pairBp4TxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairBp4TxtColor)
        }

    var pairSumTxtColor: Int = Color.parseColor("#000000")
        @Bindable get
        set(value){
            field = value
            notifyPropertyChanged(BR.pairSumTxtColor)
        }



}