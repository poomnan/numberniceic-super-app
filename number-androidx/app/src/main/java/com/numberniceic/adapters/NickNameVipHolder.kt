package com.numberniceic.adapters


import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.nickname.NameNickVipItem


import android.widget.TextView


class NickNameVipHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {



    fun bind(part: Any, position: Int) {


        if (part is NameNickVipItem) {
            val nicknameid = itemView.findViewById<TextView>(R.id.nicknameid)
            val li_nick_vip = itemView.findViewById<View>(R.id.li_nick_vip)
            val thainame = itemView.findViewById<TextView>(R.id.thainame)
            val reangthai = itemView.findViewById<TextView>(R.id.reangthai)
            val engname = itemView.findViewById<TextView>(R.id.engname)
            val reangeng = itemView.findViewById<TextView>(R.id.reangeng)
            val leksat_thai = itemView.findViewById<TextView>(R.id.leksat_thai)
            val shadow = itemView.findViewById<TextView>(R.id.shadow)
            val leksat_eng = itemView.findViewById<TextView>(R.id.leksat_eng)
            val sex = itemView.findViewById<TextView>(R.id.sex)


            val colorx = when {
                position%2 == 0 -> ContextCompat.getColor(itemView.context, R.color.colorBGGray)
                else -> ContextCompat.getColor(itemView.context, R.color.colorBGWhite)
            }

            nicknameid.text = position.toString()
            li_nick_vip.setBackgroundColor(colorx)

            thainame.text = part.thainame
            reangthai.text = part.reangthai
            engname.text = part.engname
            reangeng.text = part.reangeng
            leksat_thai.text = part.leksat_thai
            shadow.text = part.shadow
            leksat_eng.text = part.leksat_eng


            sex.text = if(part.sex == "girl") "ญ" else "ช"



        }




    }

}
