package com.numberniceic.adapters


import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.namesur.HeadMiraNameSur

import com.numberniceic.data.tabian.PairsMiracle
import com.battleent.ribbonviews.RibbonLayout
import android.widget.TextView


class NameSurMiraHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {

    fun bind(part: Any) {

        if (part is HeadMiraNameSur) {
                //header dummy
        }


        if (part is PairsMiracle) {
            val txtDesc = itemView.findViewById<TextView>(R.id.txtDesc)
            val ribbonLayout = itemView.findViewById<RibbonLayout>(R.id.ribbonLayout)
            
            txtDesc.text = part.miracledetail
            //itemView.txtHeadTitle.text = part.miracledesc

            val des = part.miracledesc
            ribbonLayout.setHeaderText(if (part.pairtype!![0].toString() == "R") "คู่นี้ร้าย $des" else "คู่นี้ดี $des")
            if (part.pairtype[0].toString() == "D") ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, this.colorPairSat(part.pairnumber!!, part)))
            else ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.colorPairR))
        }

    }


    private fun colorPairSat(pair: String, pairMiracle: PairsMiracle): Int {

        if (pairMiracle.pairnumber == pair) {
            val colorx = if (pairMiracle.pairnumber == "26" || pairMiracle.pairnumber == "62" || pairMiracle.pairnumber == "23" || pairMiracle.pairnumber == "32" || pairMiracle.pairnumber == "40" || pairMiracle.pairnumber == "04") R.color.color_txt_nocircle_tabian
            else when (pairMiracle.pairtype) {
                "D10" -> R.color.colorPairD
                "D8" -> R.color.colorPairD
                "D5" -> R.color.colorPairD
                "R10" -> R.color.colorPairR
                "R7" -> R.color.colorPairR
                "R5" -> R.color.colorPairR
                else -> R.color.color_txt_nocircle_tabian

            }

            return colorx
        }


        return R.color.color_txt_nocircle_tabian
    }

}
