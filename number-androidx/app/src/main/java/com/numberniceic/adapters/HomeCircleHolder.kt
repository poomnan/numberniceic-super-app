package com.numberniceic.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.databinding.ItemCirclePairBinding
import com.numberniceic.utils.HomeContextManager
import com.numberniceic.utils.TabianContextManager
import com.numberniceic.R

class HomeCircleHolder(itemView: View): RecyclerView.ViewHolder(itemView){


        fun bind(pair: String, type: String?){

            val txtCirclePair = itemView.findViewById<android.widget.TextView>(R.id.txtCirclePair)
            txtCirclePair.text = pair
            val color = if (type != null) HomeContextManager.getColorBG(type, itemView.context) else null
            
            if (color != null) {
                 txtCirclePair.setBackgroundColor(color)
            } else {
                 txtCirclePair.setBackgroundColor(android.graphics.Color.LTGRAY)
            }

        }
    }
