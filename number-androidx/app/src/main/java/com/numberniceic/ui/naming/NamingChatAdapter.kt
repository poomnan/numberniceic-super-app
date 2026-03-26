package com.numberniceic.ui.naming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.naming.NameMiracle
import androidx.core.view.isVisible

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val nameData: List<NameMiracle>? = null
)

class NamingChatAdapter : RecyclerView.Adapter<NamingChatAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_naming_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val containerAssistant = itemView.findViewById<View>(R.id.containerAssistant)
        private val containerUser = itemView.findViewById<View>(R.id.containerUser)
        private val txtAssistantMessage = itemView.findViewById<TextView>(R.id.txtAssistantMessage)
        private val txtUserMessage = itemView.findViewById<TextView>(R.id.txtUserMessage)
        private val layoutNameList = itemView.findViewById<LinearLayout>(R.id.layoutNameList)

        fun bind(msg: ChatMessage) {
            if (msg.isUser) {
                containerAssistant.isVisible = false
                containerUser.isVisible = true
                txtUserMessage.text = msg.text
                layoutNameList.isVisible = false
            } else {
                containerAssistant.isVisible = true
                containerUser.isVisible = false
                txtAssistantMessage.isVisible = msg.text.isNotEmpty()
                txtAssistantMessage.text = msg.text

                // Name Data
                if (!msg.nameData.isNullOrEmpty()) {
                    layoutNameList.isVisible = true
                    layoutNameList.removeAllViews()
                    msg.nameData.forEach { name ->
                        val view = LayoutInflater.from(itemView.context).inflate(R.layout.item_naming_result, layoutNameList, false)
                        
                        val gradeThai = mapOf(
                            "D10" to "ดีเยี่ยม", "D8" to "ดีมาก", "D5" to "ดี",
                            "R10" to "ร้ายมาก", "R7" to "ร้าย", "R5" to "ค่อนข้างร้าย"
                        )
                        
                        // Main Name & Meaning
                        view.findViewById<TextView>(R.id.txtName).text = name.thName
                        view.findViewById<TextView>(R.id.txtMeaning).text = name.meaning

                        // Grade Logic Helper for Pill Colors
                        fun getPillColorRes(grade: String): Int {
                            return when {
                                grade.startsWith("D") -> R.drawable.bg_green_pill
                                grade.startsWith("R") -> R.drawable.bg_red_pill
                                else -> R.drawable.bg_gray_pill
                            }
                        }

                        fun getCircleColorRes(grade: String): Int {
                            return when {
                                grade.startsWith("D") -> R.drawable.bg_circle_dark_green
                                grade.startsWith("R") -> R.drawable.bg_circle_dark_red
                                else -> R.drawable.bg_circle_dark_gray
                            }
                        }
                        
                        // Sat Pill
                        val pillSat = view.findViewById<LinearLayout>(R.id.pillSat)
                        val txtSatScore = view.findViewById<TextView>(R.id.txtSatScore)
                        val txtSatResult = view.findViewById<TextView>(R.id.txtSatResult)
                        val txtSatDesc = view.findViewById<TextView>(R.id.txtSatDesc)
                        
                        pillSat.setBackgroundResource(getPillColorRes(name.satGrade))
                        txtSatScore.setBackgroundResource(getCircleColorRes(name.satGrade))
                        txtSatScore.text = "${name.satSum}"
                        txtSatResult.text = gradeThai[name.satGrade] ?: name.satGrade
                        txtSatDesc.text = name.satDesc

                        // Sha Pill
                        val pillSha = view.findViewById<LinearLayout>(R.id.pillSha)
                        val txtShaScore = view.findViewById<TextView>(R.id.txtShaScore)
                        val txtShaResult = view.findViewById<TextView>(R.id.txtShaResult)
                        val txtShaDesc = view.findViewById<TextView>(R.id.txtShaDesc)

                        pillSha.setBackgroundResource(getPillColorRes(name.shaGrade))
                        txtShaScore.setBackgroundResource(getCircleColorRes(name.shaGrade))
                        txtShaScore.text = "${name.shaSum}"
                        txtShaResult.text = gradeThai[name.shaGrade] ?: name.shaGrade
                        txtShaDesc.text = name.shaDesc

                        // Overall Grade Badge (Top Right)
                        val txtGrade = view.findViewById<TextView>(R.id.txtGrade)
                        when {
                            name.satGrade == "D10" && name.shaGrade == "D10" -> {
                                txtGrade.visibility = View.VISIBLE
                                txtGrade.text = "มงคลสูงสุด"
                                txtGrade.setBackgroundResource(R.drawable.bg_green_pill)
                            }
                            name.satGrade.startsWith("D") && name.shaGrade.startsWith("D") -> {
                                txtGrade.visibility = View.VISIBLE
                                txtGrade.text = "มงคลดีเยี่ยม"
                                txtGrade.setBackgroundResource(R.drawable.bg_green_pill)
                            }
                            name.satGrade.startsWith("D") || name.shaGrade.startsWith("D") -> {
                                txtGrade.visibility = View.VISIBLE
                                txtGrade.text = "มงคล"
                                txtGrade.setBackgroundResource(R.drawable.bg_green_pill)
                                txtGrade.alpha = 0.8f
                            }
                            else -> {
                                txtGrade.visibility = View.GONE
                            }
                        }

                        layoutNameList.addView(view)
                    }
                } else {
                    layoutNameList.isVisible = false
                }
            }
        }
    }
}
