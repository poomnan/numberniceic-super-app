package com.numberniceic.adapters


import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.data.phone.*

import com.battleent.ribbonviews.RibbonLayout

import kotlin.coroutines.coroutineContext


class MiraclePhoneHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!){

        fun bind(part: Any){

                if (part is DataSortByTypeMiracle){
                        val txtDesc = itemView.findViewById<TextView>(R.id.txtDesc)
                        val txtHeadTitle = itemView.findViewById<TextView>(R.id.txtHeadTitle)
                        val ribbonLayout = itemView.findViewById<RibbonLayout>(R.id.ribbonLayout)

                        txtDesc.text = part.detail
                        txtHeadTitle.text = part.description
                        ribbonLayout.setHeaderText(if (part.type!![0].toString() == "R") part.number + " มีพลังอิทธิพลต่อชีวิต " +  part.percentile + "%"
                        else part.number + " มีพลังอิทธิพลต่อชีวิต " + part.percentile + "%")

                        when {
                            part.type[0].toString() == "D" -> ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.colorPairD))
                            part.type == "R10" -> ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.color_test_r10))
                            part.type == "R7" -> ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.color_test_r7))
                            part.type == "R5" -> ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.color_test_r5))
                            else -> ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.colorPairR))
                        }

                }

            if (part is PhoneWarningMsgMira){

                val msg = part.percent

                val txtPairSpecialXTitle = itemView.findViewById<TextView>(R.id.txtPairSpecialXTitle)
                val ribbon_specialx = itemView.findViewById<RibbonLayout>(R.id.ribbon_specialx)
                
                txtPairSpecialXTitle.text = part.msg
                ribbon_specialx.setHeaderText("$msg% เป็นค่าความเสี่ยง มีอิทธิพลต่อชีวิต $msg%")


            }

                if (part is PairSpecialX){

                    val msg = part.percentTotalOfTotal!!.percentTotalR - part.percentOriginR


                        var messageSnackX = ""
                        var messageSnackY = ""

                        var statusMessagex = 0
                        var statusMessagey = 0


                        if(part.scoreDupMi > 0){
                                statusMessagex = 1
                                messageSnackX = itemView.context.resources.getString(R.string.dup_miracle)
                        }

                        if(part.countPairZero > 1){
                                statusMessagey = 1
                                messageSnackY = itemView.context.resources.getString(R.string.str_countPairZero)

                        }

                        val txtPairSpecialXTitle = itemView.findViewById<TextView>(R.id.txtPairSpecialXTitle)
                        val ribbon_specialx = itemView.findViewById<RibbonLayout>(R.id.ribbon_specialx)
                        
                        if (statusMessagex == 1 && statusMessagey == 1){

                            val strCat = StringBuilder()
                            strCat.append(messageSnackX)
                            strCat.append(" และ ")
                            strCat.append(messageSnackY)

                            txtPairSpecialXTitle.text = strCat.toString()

                        }else if (statusMessagex == 1){
                                txtPairSpecialXTitle.text = messageSnackX

                        }else if (statusMessagey == 1){
                                txtPairSpecialXTitle.text = messageSnackY

                        }

                        if (part.percentTotalOfTotal.percentTotalR > 0){

                                ribbon_specialx.setHeaderText("$msg% เป็นค่าความเสี่ยง มีอิทธิพลต่อชีวิต $msg%")
                        }

                }

                if(part is PhoneCollectionDao){

                        val scoreD = StringBuilder()
                        scoreD.append("${part.miracleSummary!!.miracleD}")

                        val scoreR = StringBuilder()
                        scoreR.append("${part.miracleSummary.miracleR}")

                        itemView.findViewById<TextView>(R.id.txt_report_d).text = scoreD.toString()
                        itemView.findViewById<TextView>(R.id.txt_report_r).text = scoreR.toString()

                        when(getImgReport(part, "D")){
                                9 -> itemView.findViewById<android.widget.ImageView>(R.id.img_d).setImageResource(R.drawable.ic_vip02)
                                1 -> itemView.findViewById<android.widget.ImageView>(R.id.img_d).setImageResource(R.drawable.ic_clover)
                        }

                        when(getImgReport(part, "R")){
                                2 -> itemView.findViewById<android.widget.ImageView>(R.id.img_r).setImageResource(R.drawable.ic_evil03)
                                1 -> itemView.findViewById<android.widget.ImageView>(R.id.img_r).setImageResource(R.drawable.ic_evil02)
                        }


                }

                if(part is PercentTotalOfTotal){

                        val strCatPercetnMsgD = StringBuilder()
                        val strCatPercetnMsgR = StringBuilder()

                    strCatPercetnMsgD.append("เบอร์นี้ดี มีมงคล อิทธิพลต่อชีวิต ")
                    strCatPercetnMsgD.append(part.percentTotalD.toString())
                    strCatPercetnMsgD.append("%")
                    strCatPercetnMsgR.append("เบอร์นี้ร้าย ไม่ดี อิทธิพลต่อชีวิต ")
                    strCatPercetnMsgR.append(part.percentTotalR.toString())
                    strCatPercetnMsgR.append("%")

                        itemView.findViewById<TextView>(R.id.txt_percent_d).text = strCatPercetnMsgD.toString()
                        itemView.findViewById<TextView>(R.id.txt_percent_r).text = strCatPercetnMsgR.toString()
                }

        }

        private fun getImgReport(dao: PhoneCollectionDao, type: String):Int {
                if (type=="D") return when {
                        dao.scoreTotalOfTotal!!.scoreTotalD > 1700 -> 9
                        dao.scoreTotalOfTotal.scoreTotalD > 1000 -> 1
                        dao.scoreTotalOfTotal.scoreTotalD <= 0 -> 0
                        else -> 0
                }

                if (type=="R")
                        return when {
                                dao.scoreTotalOfTotal!!.scoreTotalR < 0 -> 2
                                dao.scoreTotalOfTotal.scoreTotalR == 0 -> 1
                                else -> 0
                        }


                return 0

        }

}
