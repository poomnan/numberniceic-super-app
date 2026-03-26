package com.numberniceic.adapters


import android.view.LayoutInflater
import android.view.ViewGroup


import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.namesur.HeadMiraNameSur

import com.numberniceic.data.tabian.PairsMiracle
import kotlin.collections.ArrayList

class NameSurMiraAdapter(private var anyList: ArrayList<Any>) : RecyclerView.Adapter<NameSurMiraHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when {
            this.anyList[position] is HeadMiraNameSur -> 0
            this.anyList[position] is PairsMiracle -> 1
            else -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameSurMiraHolder {

        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header_mira_name, parent, false)
                NameSurMiraHolder(view)
            }
            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_miracle_name, parent, false)
                NameSurMiraHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_footer, parent, false)
                NameSurMiraHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return anyList.size
    }

    override fun onBindViewHolder(holder: NameSurMiraHolder, position: Int) {
        if (getItemViewType(position) == 0){
            (holder).bind(anyList[position] as HeadMiraNameSur)
        }

        if (getItemViewType(position) == 1){
            (holder).bind(anyList[position] as PairsMiracle)
        }



    }


}


