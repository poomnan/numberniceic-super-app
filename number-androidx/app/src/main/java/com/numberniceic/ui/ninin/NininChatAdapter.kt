package com.numberniceic.ui.ninin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.ninin.NininDreamData
import androidx.core.view.isVisible

data class ChatMessage(
    val text: String, 
    val isUser: Boolean, 
    val dreamData: com.numberniceic.data.ninin.NininDreamData? = null,
    val nameFound: Boolean = false,
    val nameData: List<com.numberniceic.data.ninin.RecommendResult>? = null,
    val showConsultButton: Boolean = false,
    val showDreamPackages: Boolean = false
)

data class DreamPackageItem(
    val code: String,
    val title: String,
    val amount: Double
)

val DREAM_PACKAGE_LIFETIME = DreamPackageItem(
    code = "dream_lifetime",
    title = "ตลอดชีวิต",
    amount = 1599.0
)

val DREAM_PACKAGE_YEARLY = DreamPackageItem(
    code = "dream_yearly",
    title = "รายปี",
    amount = 299.0
)

val DREAM_PACKAGE_MONTHLY = DreamPackageItem(
    code = "dream_monthly",
    title = "รายเดือน",
    amount = 39.0
)

class NininChatAdapter(
    private val onDreamPackageClick: (DreamPackageItem) -> Unit = {},
    private val onSecretCodeSubmit: (String) -> Unit = {}
) : RecyclerView.Adapter<NininChatAdapter.MessageViewHolder>() {

    val messages = mutableListOf<ChatMessage>()

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun hasDreamPackageMessage(): Boolean {
        return messages.any { it.showDreamPackages }
    }

    fun clearDreamPackageMessages() {
        val hadPackages = messages.removeAll { it.showDreamPackages }
        if (hadPackages) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ninin_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val containerNinin = itemView.findViewById<View>(R.id.containerNinin)
        private val containerUser = itemView.findViewById<View>(R.id.containerUser)
        private val txtNininMessage = itemView.findViewById<TextView>(R.id.txtNininMessage)
        private val txtUserMessage = itemView.findViewById<TextView>(R.id.txtUserMessage)
        private val cardDreamData = itemView.findViewById<View>(R.id.cardDreamData)
        private val txtDreamKeyword = itemView.findViewById<TextView>(R.id.txtDreamKeyword)
        private val txtLuckyNumbers = itemView.findViewById<TextView>(R.id.txtLuckyNumbers)
        private val cardNameData = itemView.findViewById<View>(R.id.cardNameData)
        private val layoutNameList = itemView.findViewById<LinearLayout>(R.id.layoutNameList)
        private val btnConsultNinin = itemView.findViewById<View>(R.id.btnConsultNinin)
        private val layoutDreamPackages = itemView.findViewById<View>(R.id.layoutDreamPackages)
        private val cardDreamLife = itemView.findViewById<View>(R.id.cardDreamLife)
        private val cardDreamYear = itemView.findViewById<View>(R.id.cardDreamYear)
        private val cardDreamMonth = itemView.findViewById<View>(R.id.cardDreamMonth)
        private val btnBuyDreamLife = itemView.findViewById<View>(R.id.btnBuyDreamLife)
        private val btnBuyDreamYear = itemView.findViewById<View>(R.id.btnBuyDreamYear)
        private val btnBuyDreamMonth = itemView.findViewById<View>(R.id.btnBuyDreamMonth)
        private val edtSecretCode = itemView.findViewById<android.widget.EditText>(R.id.edtSecretCode)
        private val btnSubmitSecretCode = itemView.findViewById<View>(R.id.btnSubmitSecretCode)

        fun bind(msg: ChatMessage) {
            if (msg.isUser) {
                containerNinin.isVisible = false
                containerUser.isVisible = true
                txtUserMessage.text = msg.text
            } else {
                containerNinin.isVisible = true
                containerUser.isVisible = false
                txtNininMessage.text = msg.text
                
                // Dream Data
                if (msg.dreamData != null) {
                    cardDreamData.isVisible = true
                    txtDreamKeyword.text = "คำค้นหา: ${msg.dreamData.keyword}"
                    val ln = msg.dreamData.luckyNumbers
                    if (ln.isNullOrEmpty() || ln.equals("null", ignoreCase = true) || ln.equals("none", ignoreCase = true)) {
                        txtLuckyNumbers.text = "เลขนำโชค: ยังไม่ควรเสี่ยงโชค"
                    } else {
                        txtLuckyNumbers.text = "เลขนำโชค: $ln"
                    }
                } else {
                    cardDreamData.isVisible = false
                }

                // Name Data
                if (msg.nameFound && msg.nameData != null) {
                    cardNameData.isVisible = true
                    layoutNameList.removeAllViews()
                    msg.nameData.forEach { name ->
                        val view = LayoutInflater.from(itemView.context).inflate(R.layout.item_ninin_name_suggestion, layoutNameList, false)
                        view.findViewById<TextView>(R.id.txtName).text = name.thName
                        view.findViewById<TextView>(R.id.txtScore).text = "รหัสศาสตร์: ${name.nameSatSum}, ${name.nameShaSum}"
                        view.setOnClickListener {
                            // TODO: Open Name Detail
                        }
                        layoutNameList.addView(view)
                    }
                } else {
                    cardNameData.isVisible = false
                }
                
                btnConsultNinin.isVisible = msg.showConsultButton
                btnConsultNinin.setOnClickListener {
                    try {
                        val intent = android.content.Intent(itemView.context, com.numberniceic.ui.ChatComposeActivity::class.java)
                        itemView.context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                layoutDreamPackages.isVisible = msg.showDreamPackages
                if (msg.showDreamPackages) {
                    cardDreamLife.setOnClickListener { onDreamPackageClick(DREAM_PACKAGE_LIFETIME) }
                    cardDreamYear.setOnClickListener { onDreamPackageClick(DREAM_PACKAGE_YEARLY) }
                    cardDreamMonth.setOnClickListener { onDreamPackageClick(DREAM_PACKAGE_MONTHLY) }
                    btnBuyDreamLife.setOnClickListener { onDreamPackageClick(DREAM_PACKAGE_LIFETIME) }
                    btnBuyDreamYear.setOnClickListener { onDreamPackageClick(DREAM_PACKAGE_YEARLY) }
                    btnBuyDreamMonth.setOnClickListener { onDreamPackageClick(DREAM_PACKAGE_MONTHLY) }

                    btnSubmitSecretCode.setOnClickListener {
                        showSecretCodeDialog()
                    }

                    edtSecretCode.setOnClickListener {
                        showSecretCodeDialog()
                    }
                }
            }
        }

        private fun showSecretCodeDialog() {
            val context = itemView.context
            val input = android.widget.EditText(context).apply {
                hint = "กรอก VIP Code"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                maxLines = 1
                setSingleLine(true)
                setPadding(40, 28, 40, 28)
            }

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("ปลดล็อกด้วย VIP Code")
                .setView(input)
                .setPositiveButton("ปลดล็อก") { _, _ ->
                    val code = input.text?.toString()?.trim().orEmpty()
                    if (code.isNotEmpty()) {
                        onSecretCodeSubmit(code)
                    } else {
                        android.widget.Toast.makeText(context, "กรุณากรอกรหัส VIP Code", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("ยกเลิก", null)
                .show()
        }
    }
}
