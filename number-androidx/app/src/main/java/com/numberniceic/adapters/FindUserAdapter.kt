package com.numberniceic.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.admin.UserZ
import com.numberniceic.utils.PersonContextManager
import com.numberniceic.utils.UserContextManager
import org.joda.time.DateTime


class FindUserAdapter(private val usersList:List<Any>, private val itemClickListener: OnItemClickListener): RecyclerView.Adapter<FindUserAdapter.FindUserAdapterHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FindUserAdapterHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_find_username, parent, false)

        return FindUserAdapterHolder(itemView)
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    override fun onBindViewHolder(holder: FindUserAdapterHolder, position: Int) {
        (holder).binding(this.usersList[position] as UserZ, position, itemClickListener)

    }

    class FindUserAdapterHolder(itemView:View):RecyclerView.ViewHolder(itemView) {

        private lateinit var userId: TextView
        private lateinit var userName: TextView
        private lateinit var fullName: TextView
        private lateinit var linearFindUser: LinearLayout

        private lateinit var currentAge: TextView

        fun binding(item: Any, position: Int, listener: OnItemClickListener){
                if (item is UserZ){
                    this.userId = itemView.findViewById(R.id.txt_user_id)
                    this.userName = itemView.findViewById(R.id.txt_user_name)
                    this.fullName = itemView.findViewById(R.id.txt_full_name)
                    this.linearFindUser = itemView.findViewById(R.id.linear_finduser)
                    this.currentAge = itemView.findViewById(R.id.txt_current_age)

                    this.userId.text = if (item.memberId == "all") "ALL" else "#${item.memberId}"
                    this.userName.text = item.userName

                    if (item.memberId == "all") {
                        this.userId.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.grade_color)) // Orange-ish
                        this.linearFindUser.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorPrimary)) // Green
                        this.userId.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                        this.userName.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                        this.fullName.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                        this.currentAge.visibility = View.GONE
                    } else {
                        // Reset to default
                        this.userId.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.holo_blue_light))
                        this.linearFindUser.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                        this.userId.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                        this.userName.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                        this.fullName.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                        this.currentAge.visibility = View.VISIBLE
                    }

                    // Display full name
                    val fullNameText = buildString {
                        if (!item.realName.isNullOrEmpty()) {
                            append(item.realName)
                        }
                        if (!item.surName.isNullOrEmpty()) {
                            if (isNotEmpty()) append(" ")
                            append(item.surName)
                        }
                    }
                    this.fullName.text = if (fullNameText.isNotEmpty()) fullNameText else "ไม่ระบุชื่อ"

                    if (item.memberId != "all") {
                        if (!item.birthDat.isNullOrEmpty()) {
                            try {
                                val dt = DateTime.parse(item.birthDat)
                                val age = PersonContextManager.ageCurrent(dt.year, dt.monthOfYear, dt.dayOfMonth)
                                this.currentAge.text = "อายุปัจุบัน $age ปี"
                            } catch (e: Exception) {
                                this.currentAge.text = "ไม่ระบุวันเกิด"
                            }
                        } else {
                            this.currentAge.text = "ไม่ระบุวันเกิด"
                        }
                    }

                    itemView.setOnClickListener {
                        listener.itemClick(item)
                    }
                }



        }

    }

    interface OnItemClickListener {
        fun itemClick(userZ: UserZ)
    }



}

