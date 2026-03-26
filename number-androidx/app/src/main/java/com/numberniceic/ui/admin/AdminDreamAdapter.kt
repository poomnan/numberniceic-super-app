package com.numberniceic.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.admin.DreamAdminItem

class AdminDreamAdapter(
    private val onEdit: (DreamAdminItem) -> Unit,
    private val onDelete: (DreamAdminItem) -> Unit
) : RecyclerView.Adapter<AdminDreamAdapter.ViewHolder>() {

    private var items = listOf<DreamAdminItem>()

    fun setItems(newItems: List<DreamAdminItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_dream, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtKeyword: TextView = view.findViewById(R.id.txtKeyword)
        val txtMeaning: TextView = view.findViewById(R.id.txtMeaning)
        val txtLuckyNumbers: TextView = view.findViewById(R.id.txtLuckyNumbers)
        val txtCategory: TextView = view.findViewById(R.id.txtCategory)
        val txtVectorStatus: TextView = view.findViewById(R.id.txtVectorStatus)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(item: DreamAdminItem) {
            txtKeyword.text = item.dreamKeyword
            txtMeaning.text = item.dreamInterpretation
            txtLuckyNumbers.text = "เลขเด็ด: ${if (item.luckyNumbers.isNotEmpty()) item.luckyNumbers else "-"}"
            txtCategory.text = item.category ?: "ความฝัน"
            txtCategory.setBackgroundColor(if (item.category == "ปรึกษา") 0xFFF3E5F5.toInt() else 0xFFE1F5FE.toInt())
            txtCategory.setTextColor(if (item.category == "ปรึกษา") 0xFF7B1FA2.toInt() else 0xFF0277BD.toInt())
            
            txtVectorStatus.visibility = if (item.hasVector) View.VISIBLE else View.GONE

            btnEdit.setOnClickListener { onEdit(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
