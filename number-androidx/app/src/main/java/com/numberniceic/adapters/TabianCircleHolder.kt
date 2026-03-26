package com.numberniceic.adapters

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.databinding.ItemCirclePairBinding
import com.numberniceic.utils.TabianContextManager
import android.widget.TextView

class TabianCircleHolder(itemView: View): RecyclerView.ViewHolder(itemView){


        fun bind(pair: String, type: String){

            val txtCirclePair = itemView.findViewById<TextView>(R.id.txtCirclePair)
            txtCirclePair.text = pair

            if (pair == "25"){
                txtCirclePair.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.bg_special))
            }
            else if(pair == "26"){
                txtCirclePair.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.bg_special2))
            }
            else if(pair == "23" || pair == "32" || pair == "35" || pair == "53"){
                txtCirclePair.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.bg_special3))
            }else{
            txtCirclePair.setBackgroundColor(TabianContextManager.getColorBG(type, itemView.context)!!)

            }


        }



    }
