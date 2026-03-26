package com.numberniceic.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.databinding.ItemCirclePairBinding

class TabianPairAdapter(private var pairs: ArrayList<String>, private var pairsMiracle: ArrayList<PairsMiracle>): RecyclerView.Adapter<TabianCircleHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): TabianCircleHolder {


        val inf = LayoutInflater.from(parent.context).inflate(R.layout.item_circle_pair, parent, false)

        return TabianCircleHolder(inf)
    }

    override fun getItemCount(): Int {
        return pairs.size
    }

    override fun onBindViewHolder(holder: TabianCircleHolder, position: Int) {

        for (value in this.pairsMiracle){
            if (this.pairs[position] == value.pairnumber){
                holder.bind(pairs[position], value.pairtype!![0].toString())
            }
        }



    }
}