package com.numberniceic.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.news.News
import com.numberniceic.ui.tambon.ChangeNumf
import com.numberniceic.ui.tambon.TambolAct
import com.numberniceic.ui.tambon.Tambonf


class ChangeNumAdapter(private val newsList: List<Any>, private val changeNumf: ChangeNumf) : RecyclerView.Adapter<ChangeNumAdapter.ChangeNumHolder>() {



    override fun getItemViewType(position: Int): Int {
        return if (newsList[position] is String) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChangeNumHolder {

        val view = if (viewType == 0) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_pra_preang_header, parent, false)

        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_change_num, parent, false)

        }

        return ChangeNumHolder(view)

    }

    override fun getItemCount(): Int {
        return newsList.size
    }

    override fun onBindViewHolder(holder: ChangeNumHolder, position: Int) {

        if (getItemViewType(position) == 1){
            (holder).binding(newsList[position] as News, changeNumf, position)

        }else{
            (holder).binding(newsList[position] as String, changeNumf, position)

        }
    }


    class ChangeNumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun binding(item: Any, changeNumf: ChangeNumf, position: Int){
                if (item is News){
                    val txtHeader = itemView.findViewById<android.widget.TextView>(R.id.txt_header_changenum)
                    val linearItem = itemView.findViewById<android.view.View>(R.id.linear_changenum_item)
                    
                    txtHeader.text = item.newsHeader
                    itemView.setOnClickListener {

                        val intent = Intent(itemView.context, TambolAct::class.java)
                        intent.putExtra("tambonId", item.newsId)
                        intent.putExtra("type_title", item.newsHeader)
                        intent.putExtra("type", "change_num")
                        changeNumf.startActivity(intent)
                        //diaf.dismiss()

                    }

                    // linearItem.setBackgroundColor(ContextCompat.getColor(itemView.context, if (position%2 == 0) R.color.colorBGWhite else R.color.colorBGDay))

                }
                if (item is String){

                    val imgHeader = itemView.findViewById<android.widget.ImageView>(R.id.img_prapreang_header)
                    val txtTitle = itemView.findViewById<android.widget.TextView>(R.id.txt_title_header)
                    
                    imgHeader.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.buddha_light))
                    txtTitle.text = "ขั้นตอนการเปลี่ยนแปลงตัวเลข"

                }
            }
    }


}
