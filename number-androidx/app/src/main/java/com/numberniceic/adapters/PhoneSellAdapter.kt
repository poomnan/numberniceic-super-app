package com.numberniceic.adapters


import android.view.LayoutInflater
import android.view.ViewGroup


import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.home.ScoreRD
import com.numberniceic.data.phone.Miracle
import com.numberniceic.data.phone.PhoneNumberItem
import com.numberniceic.data.phone.PhoneSellHeader

import com.numberniceic.data.tabian.PairsMiracle
import kotlin.collections.ArrayList

class PhoneSellAdapter(private var anyList: ArrayList<Any>) : RecyclerView.Adapter<PhoneSellHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when {
            this.anyList[position] is PhoneSellHeader -> 0
            this.anyList[position] is PhoneNumberItem -> 1
            else -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneSellHolder {

        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_head_phonesell, parent, false)
                PhoneSellHolder(view)
            }
            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_phonesell, parent, false)
                PhoneSellHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_footer, parent, false)
                PhoneSellHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return anyList.size
    }

    override fun onBindViewHolder(holder: PhoneSellHolder, position: Int) {

        if (getItemViewType(position) == 0){
            (holder).bind(anyList[position] as PhoneSellHeader)
        }

        if (getItemViewType(position) == 1){
            (holder).bind(anyList[position] as PhoneNumberItem)
        }



    }


}


