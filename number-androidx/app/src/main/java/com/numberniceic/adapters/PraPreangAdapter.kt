package com.numberniceic.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.news.News
import com.numberniceic.ui.tambon.TambolAct
import com.numberniceic.ui.tambon.TambonDiaf
import com.numberniceic.ui.tambon.Tambonf
import com.numberniceic.utils.PersonContextManager
import android.widget.TextView
import android.widget.ImageView

class PraPreangAdapter(private val newsList: List<Any>,private val diaf: Tambonf) : RecyclerView.Adapter<PraPreangAdapter.PraPreangHolder>() {



    override fun getItemViewType(position: Int): Int {
        return if (newsList[position] is String) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PraPreangHolder {

        val view = if (viewType == 0) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_pra_preang_header, parent, false)

        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_pra_preang, parent, false)

        }

        return PraPreangHolder(view)

    }

    override fun getItemCount(): Int {
        return newsList.size
    }

    override fun onBindViewHolder(holder: PraPreangHolder, position: Int) {

        if (getItemViewType(position) == 1){
            (holder).binding(newsList[position] as News, diaf, position)

        }else{
            (holder).binding(newsList[position] as String, diaf, position)

        }
    }


    class PraPreangHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun binding(item: Any, diaf: Tambonf, position: Int){
                if (item is News){
                    val txtHeader = itemView.findViewById<TextView>(R.id.txt_header_prapreang)
                    val linearItem = itemView.findViewById<View>(R.id.linear_news_item)
                    
                    txtHeader.text = item.newsHeader
                    itemView.setOnClickListener {

                        val intent = Intent(itemView.context, TambolAct::class.java)
                        intent.putExtra("tambonId",item.newsId!!)

                        intent.putExtra("type_title", "ขั้นตอนวิธีการทำบุญ")
                        intent.putExtra("type", "tambon")
                        diaf.startActivity(intent)



                        //diaf.dismiss()

                    }

                    // linearItem.setBackgroundColor(ContextCompat.getColor(itemView.context, if (position%2 == 0) R.color.colorBGWhite else R.color.colorBGDay))

                }
                if (item is String){

                    val imgHeader = itemView.findViewById<ImageView>(R.id.img_prapreang_header)
                    val txtTitleHeader = itemView.findViewById<TextView>(R.id.txt_title_header)
                    
                    imgHeader.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.buddha_light))
                    txtTitleHeader.text = "วิธีการทำบุญคุ้มครองดวงชะตา"

                }
            }
    }


}
