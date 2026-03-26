package com.numberniceic.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.numberniceic.R
import com.numberniceic.data.admin.Userx

class UserContextManager {

    companion object {

        fun isAdmin(user: Userx?): Boolean {
            if (user == null) return false
            
            val statusNormalized = user.status?.lowercase()?.trim() ?: ""
            val vipcodeNormalized = user.vipcode?.lowercase()?.trim() ?: ""
            
            return statusNormalized.contains("admin") || 
                   statusNormalized == "9" ||
                   statusNormalized == "99" ||
                   vipcodeNormalized.contains("admin") || 
                   vipcodeNormalized == "administrator"
        }

        fun isAdmin(context: Context): Boolean {
            val user = userX(context)
            return isAdmin(user)
        }

        fun userX(context: Context): Userx? {
            var userx: Userx? = null
            val sharedref = context.getSharedPreferences("userdata", Context.MODE_PRIVATE)
            val userjson = sharedref.getString("json", null)

            if (!userjson.isNullOrEmpty()) {
                try {
                    userx = Gson().fromJson(userjson, Userx::class.java)
                } catch (e: Exception) {
                    android.util.Log.e("UserContextManager", "Error parsing json: ${e.message}")
                }
            }

            // Fallback to individual fields if JSON is missing or failed
            if (userx == null || userx.userId.isNullOrEmpty()) {
                val savedId = sharedref.getString("saved_userid", null) 
                    ?: sharedref.getString("userid", null)
                    ?: sharedref.getString("memberid", null)
                val savedName = sharedref.getString("saved_realname", null)
                val savedUser = sharedref.getString("saved_username", null)
                
                if (!savedId.isNullOrEmpty()) {
                    userx = Userx(
                        userId = savedId,
                        realName = savedName,
                        surname = null,
                        username = savedUser,
                        birthDay = null,
                        sHour = 0,
                        sMinute = 0,
                        sGender = null,
                        ageYear = 0,
                        ageMonth = 0,
                        ageWeek = 0,
                        ageDay = 0,
                        password = null,
                        status = sharedref.getString("status", null),
                        vipcode = sharedref.getString("vipcode", null) ?: "normal",
                        sProvince = null,
                        avatar = null,
                    )
                }
            }

            return userx
        }

        fun imgVip(type: String): Int {
            return when (type) {
                "SpecialP" -> R.drawable.tambon
                "silver" -> R.drawable.icon_vip_beauty
                "gold" -> R.drawable.icon_gold01
                "diamond" -> R.drawable.diamond
                "admin" -> R.drawable.icon_admin
                else -> R.drawable.icon_member
            }

        }

        fun getAvatarResId(avatarId: String?): Int {
            return when (avatarId) {
                "10" -> R.drawable.ic_avatar_10
                "11" -> R.drawable.ic_avatar_11
                "12" -> R.drawable.ic_avatar_12
                "13" -> R.drawable.ic_avatar_13
                else -> R.drawable.ic_avatar_10 // Default avatar
            }
        }

    }

}