package com.numberniceic.adapters


import android.content.res.Resources
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.home.ScoreRD
import com.numberniceic.data.namesur.HeadMiraNameNick

import com.numberniceic.data.phone.DataSortByTypeMiracle
import com.numberniceic.data.phone.Miracle
import com.numberniceic.data.tabian.PairsMiracle
import com.battleent.ribbonviews.RibbonLayout
import android.widget.TextView


class NameNickMiraHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {

    fun bind(part: Any) {
        if (part is PairsMiracle) {
            val txtDesc = itemView.findViewById<TextView>(R.id.txtDesc)
            val ribbonLayout = itemView.findViewById<RibbonLayout>(R.id.ribbonLayout)
            
            txtDesc.text = part.miracledetail
            //itemView.txtHeadTitle.text = part.miracledesc

            val des = part.miracledesc
            ribbonLayout.setHeaderText(if (part.pairtype!![0].toString() == "R") "คู่นี้ร้าย $des" else "คู่นี้ดี $des")

            if (part.pairtype[0].toString() == "D") ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, this.colorPairSat(part.pairnumber!!, part)))
            else ribbonLayout.setHeaderRibbonColor(ContextCompat.getColor(itemView.context, R.color.colorPairR))

        } else if (part is HeadMiraNameNick) {
            itemView.findViewById<TextView>(R.id.name_sat).text = part.nickname
            itemView.findViewById<TextView>(R.id.sumnumsat_all).text = part.satNickname
            itemView.findViewById<TextView>(R.id.sumall_sha).text = part.shaNickName

        }

    }


    private fun colorPairSat(pair: String, pairMira: PairsMiracle): Int {

        if (pairMira.pairnumber == pair) {
            val colorx = if (pairMira.pairnumber == "26" || pairMira.pairnumber == "62" || pairMira.pairnumber == "23" || pairMira.pairnumber == "32" || pairMira.pairnumber == "40" || pairMira.pairnumber == "04") R.color.color_txt_nocircle_tabian
            else when (pairMira.pairtype) {
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
