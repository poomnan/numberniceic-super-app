package com.numberniceic.adapters


import android.content.res.Resources
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.home.HomeHeaderMira
import com.numberniceic.data.home.ScoreRD

import com.numberniceic.data.phone.DataSortByTypeMiracle
import com.numberniceic.data.phone.Miracle
import com.numberniceic.data.tabian.PairsMiracle
import com.battleent.ribbonviews.RibbonLayout
import java.text.NumberFormat


class HomeMiraHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!){

        fun bind(part: Any){

                if (part is PairsMiracle){
                        val txtDesc = itemView.findViewById<android.widget.TextView>(R.id.txtDesc)
                        txtDesc.text = part.miracledetail
                        //itemView.txtHeadTitle.text = part.miracledesc

                        val des = part.miracledesc
                        val ribbonLayout = itemView.findViewById<RibbonLayout>(R.id.ribbonLayout)
                        
                        ribbonLayout.setHeaderText(if (part.pairtype!![0].toString() == "R") "คู่นี้ร้าย $des" else "คู่นี้ดี $des")
                        if (part.pairtype[0].toString() == "D") ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.colorPairD))
                        else ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.colorPairR))
                }

                else if(part is HomeHeaderMira){
                        val txtScoreD = itemView.findViewById<android.widget.TextView>(R.id.txt_score_header_home_d)
                        val txtScoreR = itemView.findViewById<android.widget.TextView>(R.id.txt_score_header_home_r)
                        val txtReportD = itemView.findViewById<android.widget.TextView>(R.id.txt_report_header_home_d)
                        val txtReportR = itemView.findViewById<android.widget.TextView>(R.id.txt_report_header_home_r)

                        txtScoreD.text = NumberFormat.getInstance().format(part.scoredD).toString()
                        txtScoreR.text = NumberFormat.getInstance().format(part.scoreR).toString()

                        txtReportD.text = part.reportD
                        txtReportR.text = part.reportR


                }

        }

}
