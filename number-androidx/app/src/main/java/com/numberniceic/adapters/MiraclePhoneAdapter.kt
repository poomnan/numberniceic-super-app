package com.numberniceic.adapters


import android.view.LayoutInflater
import android.view.ViewGroup

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.data.phone.*


import kotlin.collections.ArrayList

class MiraclePhoneAdapter(private var anyList: ArrayList<Any>) : RecyclerView.Adapter<MiraclePhoneHolder>() {


    override fun getItemViewType(position: Int): Int {
        return when {
            this.anyList[position] is PhoneCollectionDao -> 0
            this.anyList[position] is DataSortByTypeMiracle -> 1
            this.anyList[position] is PercentTotalOfTotal -> 2
            this.anyList[position] is PairSpecialX -> 3
            this.anyList[position] is PhoneWarningMsgMira -> 4

            else -> 9

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiraclePhoneHolder {

        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
                MiraclePhoneHolder(view)
            }
            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_miracle, parent, false)
                MiraclePhoneHolder(view)
            }
            2 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header_percent, parent, false)
                MiraclePhoneHolder(view)
            }

            3 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_percentx, parent, false)
                MiraclePhoneHolder(view)
            }


            4 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_percentx, parent, false)
                MiraclePhoneHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_footer, parent, false)
                MiraclePhoneHolder(view)
            }

        }
    }

    override fun getItemCount(): Int {
        return anyList.size
    }

    override fun onBindViewHolder(holder: MiraclePhoneHolder, position: Int) {


        when(getItemViewType(position)){
            0 -> (holder).bind(anyList[position] as PhoneCollectionDao)
            1 -> (holder).bind(anyList[position] as DataSortByTypeMiracle)
            2 -> (holder).bind(anyList[position] as PercentTotalOfTotal)
            3 -> (holder).bind(anyList[position] as PairSpecialX)
            4 -> (holder).bind(anyList[position] as PhoneWarningMsgMira)

        }




    }


}


