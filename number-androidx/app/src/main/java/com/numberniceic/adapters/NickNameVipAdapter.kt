package com.numberniceic.adapters


import android.view.LayoutInflater
import android.view.ViewGroup


import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.nickname.NameNickVipItem

import kotlin.collections.ArrayList

class NickNameVipAdapter(private var anyList: ArrayList<Any>) : RecyclerView.Adapter<NickNameVipHolder>() {




    override fun getItemViewType(position: Int): Int {


        return when {
            this.anyList[position] is String -> 0
            this.anyList[position] is NameNickVipItem -> 1
            else -> 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NickNameVipHolder {


        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_namenick_header, parent, false)
                NickNameVipHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_namenick, parent, false)
                NickNameVipHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return anyList.size
    }

    override fun onBindViewHolder(holder: NickNameVipHolder, position: Int) {

        if (getItemViewType(position) == 1){
            (holder).bind(anyList[position] as NameNickVipItem, position)
        }




    }






}


