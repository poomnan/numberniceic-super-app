package com.numberniceic.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.databinding.ItemCirclePairBinding

class HomePairAdapter(private var pairs: ArrayList<String>, private var pairsMiracle: ArrayList<PairsMiracle>): RecyclerView.Adapter<HomeCircleHolder>() {

    // สร้าง HashMap ครั้งเดียวเพื่อเพิ่มความเร็วในการค้นหา
    private val miracleMap: Map<String, String> = pairsMiracle.mapNotNull { miracle ->
        miracle.pairnumber?.let { pairNumber ->
            pairNumber to (miracle.pairtype?.get(0)?.toString() ?: "")
        }
    }.toMap()

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): HomeCircleHolder {
        val inf = LayoutInflater.from(parent.context).inflate(R.layout.item_circle_pair, parent, false)
        return HomeCircleHolder(inf)
    }

    override fun getItemCount(): Int {
        return pairs.size
    }

    override fun onBindViewHolder(holder: HomeCircleHolder, position: Int) {
        val pairNumber = pairs[position]
        val pairType = miracleMap[pairNumber]
        holder.bind(pairNumber, pairType)
    }
}