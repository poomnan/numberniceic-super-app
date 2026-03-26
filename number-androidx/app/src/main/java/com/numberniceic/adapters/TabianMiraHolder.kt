package com.numberniceic.adapters


import android.content.res.Resources
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R

import com.numberniceic.data.phone.DataSortByTypeMiracle
import com.numberniceic.data.phone.Miracle
import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.data.tabian.SiangTabian
import com.numberniceic.data.tabian.TabianHeaderReport
import com.numberniceic.utils.TabianContextManager
import com.battleent.ribbonviews.RibbonLayout
import android.widget.TextView
import android.widget.ImageView
import java.text.NumberFormat


class TabianMiraHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!){

        fun bind(part: Any){

                if (part is PairsMiracle){
                        val txtDesc = itemView.findViewById<TextView>(R.id.txtDesc)
                        val txtHeadTitle = itemView.findViewById<TextView>(R.id.txtHeadTitle)
                        val ribbonLayout = itemView.findViewById<RibbonLayout>(R.id.ribbonLayout)

                        txtDesc.text = part.miracledetail
                        txtHeadTitle.text = part.miracledesc

                        val percent = part.percent.toString()
                        val numx = part.pairnumber

                        ribbonLayout.setHeaderText("$numx มีพลังอิทธิพลต่อชีวิต $percent%")


                        if (part.pairtype!![0].toString() == "D") ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.colorPairD))

                        else ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.colorPairR))
                }

                else if(part is TabianHeaderReport){
                        val img_mira_report_d = itemView.findViewById<ImageView>(R.id.img_mira_report_d)
                        val txt_percent_tabian_d = itemView.findViewById<TextView>(R.id.txt_percent_tabian_d)
                        val txt_percent_tabian_r = itemView.findViewById<TextView>(R.id.txt_percent_tabian_r)
                        val txt_score_d = itemView.findViewById<TextView>(R.id.txt_score_d)
                        val txt_score_r = itemView.findViewById<TextView>(R.id.txt_score_r)
                        val txt_mira_report_d = itemView.findViewById<TextView>(R.id.txt_mira_report_d)
                        val txt_mira_report_r = itemView.findViewById<TextView>(R.id.txt_mira_report_r)

                    if(TabianContextManager.getVip(part.scoreD) == 9) img_mira_report_d.setImageResource(R.drawable.ic_vip02) else img_mira_report_d.setImageResource(R.drawable.ic_clover)

                     txt_percent_tabian_d.text = part.percentD.toString()
                     txt_percent_tabian_r.text = part.percentR.toString()

                        txt_score_d.text = NumberFormat.getInstance().format(part.scoreD).toString()
                        txt_score_r.text = NumberFormat.getInstance().format(part.scoreR).toString()

                        txt_mira_report_d.text = part.reportDetailD
                        txt_mira_report_r.text = part.reportDetailR

                }

                else if(part is SiangTabian){
                        val ribbon_specialx = itemView.findViewById<RibbonLayout>(R.id.ribbon_specialx)
                        val txtPairSpecialXTitle = itemView.findViewById<TextView>(R.id.txtPairSpecialXTitle)
                        val percentS =  part.percent.toString()
                        ribbon_specialx.setHeaderText("$percentS% เป็นความเสี่ยงมีิอิทธิพลต่อชีวิต $percentS%")
                        txtPairSpecialXTitle.text = part.messagex

                }

        }

}
