package com.numberniceic.adapters


import android.view.LayoutInflater
import android.view.ViewGroup


import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R

import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.data.tabian.SiangTabian
import com.numberniceic.data.tabian.TabianHeaderReport
import kotlin.collections.ArrayList

class TabianMiraAdapter(private var anyList: ArrayList<Any>) : RecyclerView.Adapter<TabianMiraHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when {

            this.anyList[position] is TabianHeaderReport -> 0
            this.anyList[position] is PairsMiracle -> 1
            this.anyList[position] is SiangTabian -> 2
            else -> 9
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabianMiraHolder {

        return when (viewType) {

            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header_tabian_mira, parent, false)
                TabianMiraHolder(view)
            }


            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_miracle, parent, false)
                TabianMiraHolder(view)
            }

            2 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_percentx, parent, false)
                TabianMiraHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_footer, parent, false)
                TabianMiraHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return anyList.size
    }

    override fun onBindViewHolder(holder: TabianMiraHolder, position: Int) {


        if (getItemViewType(position) == 0){
            (holder).bind(anyList[position] as TabianHeaderReport)
        }

        if (getItemViewType(position) == 1){
            (holder).bind(anyList[position] as PairsMiracle)
        }


        if (getItemViewType(position) == 2){
            (holder).bind(anyList[position] as SiangTabian)
        }



    }


}


