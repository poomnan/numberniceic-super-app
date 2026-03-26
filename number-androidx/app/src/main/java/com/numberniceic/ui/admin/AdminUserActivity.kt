package com.numberniceic.ui.admin

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.components.VvipBadgeBitmapHelper
import com.numberniceic.utils.UserContextManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun Int.dpToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}

class AdminUserActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnSearch: ImageButton
    private lateinit var rvUsers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_user)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        rvUsers = findViewById(R.id.rvUsers)
        progressBar = findViewById(R.id.progressBar)

        rvUsers.layoutManager = LinearLayoutManager(this)
        adapter = UserAdapter()
        rvUsers.adapter = adapter

        btnSearch.setOnClickListener {
            val query = etSearch.text.toString()
            fetchUsers(query)
        }

        fetchUsers("")
    }

    private fun fetchUsers(query: String) {
        progressBar.visibility = View.VISIBLE
        RetrofitClient.api.listUsers(query).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()?.get("status")?.asString
                    if (status == "success") {
                        val usersJson = response.body()?.getAsJsonArray("users")
                        val userList = mutableListOf<JsonObject>()
                        usersJson?.forEach { userList.add(it.asJsonObject) }
                        adapter.setUsers(userList)
                    }
                } else {
                    Toast.makeText(this@AdminUserActivity, "เกิดข้อผิดพลาดในการดึงข้อมูล", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AdminUserActivity, "เชื่อมต่อล้มเหลว: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStatus(memberId: String, vipcode: String) {
        val statusDisplay = when (vipcode.lowercase()) {
            "normal" -> "NORMAL"
            "vip" -> "VIP"
            "vvip" -> "VVIP"
            "mvp" -> "MVP"
            else -> vipcode.uppercase()
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("ยืนยันการเปลี่ยนสถานะ")
            .setMessage("คุณต้องการเปลี่ยนสถานะสมาชิกเป็น $statusDisplay ใช่หรือไม่?")
            .setPositiveButton("ยืนยัน") { _, _ -> performUpdateStatus(memberId, vipcode) }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun performUpdateStatus(memberId: String, vipcode: String) {
        val body = JsonObject()
        body.addProperty("memberid", memberId)
        body.addProperty("vipcode", vipcode)

        progressBar.visibility = View.VISIBLE
        RetrofitClient.api.updateUserStatus(body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()?.get("message")?.asString
                    Toast.makeText(this@AdminUserActivity, message ?: "สำเร็จ", Toast.LENGTH_SHORT).show()
                    fetchUsers(etSearch.text.toString())
                    
                    // 🚀 ปลดปล่อยสัญญาณ (Broadcast) เพื่ออัปเดต UI ทันทีหากเป็นการแก้สถานะตัวเอง
                    val currentUser = UserContextManager.userX(this@AdminUserActivity)
                    if (currentUser?.userId == memberId) {
                        Log.d("AdminUserActivity", "Changed own status! Sending NEW_NOTIFICATION broadcast.")
                        sendBroadcast(Intent("com.numberniceic.NEW_NOTIFICATION"))
                    }
                } else {
                    Toast.makeText(this@AdminUserActivity, "เกิดข้อผิดพลาดในการเปลี่ยนสถานะ", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AdminUserActivity, "เชื่อมต่อล้มเหลว: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── Helper: เริ่ม rotating badge animation บน ImageView ──────────────────
    private fun startVvipBadgeAnim(iv: ImageView, sizeDp: Int) {
        (iv.tag as? ValueAnimator)?.cancel()
        val anim = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { a ->
                val angle = a.animatedValue as Float
                try {
                    val bmp = VvipBadgeBitmapHelper.createBadgeBitmap(iv.context, sizeDp, angle)
                    iv.setImageBitmap(bmp)
                } catch (_: Exception) {}
            }
        }
        iv.tag = anim
        anim.start()
    }

    private fun stopVvipBadgeAnim(iv: ImageView) {
        (iv.tag as? ValueAnimator)?.cancel()
        iv.tag = null
    }

    inner class UserAdapter : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
        private var users = listOf<JsonObject>()

        fun setUsers(newUsers: List<JsonObject>) {
            users = newUsers
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = users[position]
            val memberId = user.get("memberid")?.asString ?: ""
            val username = user.get("username")?.asString ?: ""
            val realname = user.get("realname")?.asString ?: ""
            val surname = user.get("surname")?.asString ?: ""
            val vipcode = user.get("vipcode")?.asString ?: "normal"

            holder.tvUsername.text = username
            holder.tvRealName.text = "$realname $surname"
            holder.tvMemberId.text = "ID: $memberId"

            // ── Status Badge (มุมบนขวา) ────────────────────────────────────────
            if (vipcode == "vvip") {
                holder.tvCurrentStatus.visibility = View.GONE
                holder.ivVvipBadgeStatus.visibility = View.VISIBLE
                startVvipBadgeAnim(holder.ivVvipBadgeStatus, 36)
            } else {
                stopVvipBadgeAnim(holder.ivVvipBadgeStatus)
                holder.ivVvipBadgeStatus.visibility = View.GONE
                holder.tvCurrentStatus.visibility = View.VISIBLE
                holder.tvCurrentStatus.text = vipcode
                when (vipcode) {
                    "admin" -> {
                        holder.tvCurrentStatus.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                        holder.tvCurrentStatus.setTextColor(android.graphics.Color.WHITE)
                    }
                    "mvp" -> {
                        holder.tvCurrentStatus.setBackgroundColor(android.graphics.Color.parseColor("#FFC107"))
                        holder.tvCurrentStatus.setTextColor(android.graphics.Color.BLACK)
                    }
                    "vip" -> {
                        holder.tvCurrentStatus.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
                        holder.tvCurrentStatus.setTextColor(android.graphics.Color.WHITE)
                    }
                    else -> {
                        holder.tvCurrentStatus.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                        holder.tvCurrentStatus.setTextColor(android.graphics.Color.parseColor("#333333"))
                    }
                }
            }

            holder.btnSetNormal.setOnClickListener { updateStatus(memberId, "normal") }
            holder.btnSetVip.setOnClickListener { updateStatus(memberId, "vip") }

            // ── ปุ่ม VVIP (แถวล่าง) — bitmap หมุนขอบทอง ──────────────────────
            stopVvipBadgeAnim(holder.ivBtnVvip)
            startVvipBadgeAnim(holder.ivBtnVvip, 31)
            holder.ivBtnVvip.setOnClickListener { updateStatus(memberId, "vvip") }

            holder.btnSetGold.setOnClickListener { updateStatus(memberId, "mvp") }
        }

        override fun onViewRecycled(holder: UserViewHolder) {
            super.onViewRecycled(holder)
            stopVvipBadgeAnim(holder.ivVvipBadgeStatus)
            stopVvipBadgeAnim(holder.ivBtnVvip)
        }

        override fun getItemCount(): Int = users.size

        inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
            val tvRealName: TextView = itemView.findViewById(R.id.tvRealName)
            val tvMemberId: TextView = itemView.findViewById(R.id.tvMemberId)
            val tvCurrentStatus: TextView = itemView.findViewById(R.id.tvCurrentStatus)
            val ivVvipBadgeStatus: ImageView = itemView.findViewById(R.id.iv_vvip_badge_status)
            val btnSetNormal: Button = itemView.findViewById(R.id.btnSetNormal)
            val btnSetVip: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnSetVip)
            val ivBtnVvip: ImageView = itemView.findViewById(R.id.ivBtnVvip)
            val btnSetGold: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnSetGold)
        }
    }
}
