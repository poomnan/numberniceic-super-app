package com.numberniceic.adapters


import android.view.LayoutInflater
import android.view.ViewGroup


import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.home.ScoreRD
import com.numberniceic.data.namesur.HeadMiraNameNick
import com.numberniceic.data.phone.Miracle

import com.numberniceic.data.tabian.PairsMiracle
import kotlin.collections.ArrayList

class NameNickMiraAdapter(private var anyList: ArrayList<Any>) : RecyclerView.Adapter<NameNickMiraHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when {
            this.anyList[position] is HeadMiraNameNick -> 0
            this.anyList[position] is PairsMiracle -> 1
            else -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameNickMiraHolder {

        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header_mira_namenick, parent, false)
                NameNickMiraHolder(view)
            }
            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_miracle_name, parent, false)
                NameNickMiraHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_footer, parent, false)
                NameNickMiraHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return anyList.size
    }

    override fun onBindViewHolder(holder: NameNickMiraHolder, position: Int) {

        if (getItemViewType(position) == 0){
            (holder).bind(anyList[position] as HeadMiraNameNick)
        }

        if (getItemViewType(position) == 1){
            (holder).bind(anyList[position] as PairsMiracle)
        }



    }


}


