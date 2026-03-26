package com.numberniceic.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.admin.InauspiciousData

class InauspiciousHistoryAdapter(
    private val items: List<InauspiciousData>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<InauspiciousHistoryAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onEditClick(item: InauspiciousData)
        fun onDeleteClick(item: InauspiciousData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inauspicious_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val label = if (item.type == "year") "ปีนี้" else "ตลอดชีวิต"
        holder.txtType.text = label
        holder.txtContent.text = item.description
        holder.txtDate.text = item.assignedAt ?: ""

        holder.btnEdit.setOnClickListener { listener.onEditClick(item) }
        holder.btnDelete.setOnClickListener { listener.onDeleteClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtType: TextView = view.findViewById(R.id.txt_type)
        val txtContent: TextView = view.findViewById(R.id.txt_content)
        val txtDate: TextView = view.findViewById(R.id.txt_date)
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit_item)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_item)
    }
}
