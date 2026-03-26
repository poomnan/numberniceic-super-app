package com.numberniceic.adapters


import android.view.LayoutInflater
import android.view.ViewGroup


import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.home.HomeHeaderMira
import com.numberniceic.data.home.HomeReport
import com.numberniceic.data.home.ScoreRD
import com.numberniceic.data.phone.Miracle

import com.numberniceic.data.tabian.PairsMiracle
import kotlin.collections.ArrayList

class HomeMiraAdapter(private var anyList: ArrayList<Any>) : RecyclerView.Adapter<HomeMiraHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when {
            this.anyList[position] is HomeHeaderMira -> 0
            this.anyList[position] is PairsMiracle -> 1
            else -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeMiraHolder {

        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header_home_mira, parent, false)
                HomeMiraHolder(view)
            }
            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_miracle_home, parent, false)
                HomeMiraHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_footer, parent, false)
                HomeMiraHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return anyList.size
    }

    override fun onBindViewHolder(holder: HomeMiraHolder, position: Int) {

        if (getItemViewType(position) == 0){
            (holder).bind(anyList[position] as HomeHeaderMira)
        }

        if (getItemViewType(position) == 1){
            (holder).bind(anyList[position] as PairsMiracle)
        }



    }


}


