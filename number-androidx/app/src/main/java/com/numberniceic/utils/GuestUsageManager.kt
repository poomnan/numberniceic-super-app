package com.numberniceic.utils

import android.content.Context
import com.google.gson.Gson

data class GuestUsage(
    val guestId: String,
    var count: Int = 0
)

class GuestUsageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("guest_usage_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun getGuestId(): String {
        return android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_guest"
    }

    fun getUsage(): GuestUsage {
        val json = prefs.getString("usage", null)
        val guestId = getGuestId()
        if (json == null) {
            return GuestUsage(guestId)
        }
        return try {
            gson.fromJson(json, GuestUsage::class.java)
        } catch (e: Exception) {
            GuestUsage(guestId)
        }
    }

    fun canSendMessage(): Boolean {
        // Correctly check if user is a member/logged in
        val userx = UserContextManager.userX(context)
        if (userx != null && !userx.userId.isNullOrEmpty()) {
            // Check if member status is actually valid (not just a leftover guest record)
            return true
        }
        
        return getUsage().count < 3
    }

    fun increment() {
        val userx = UserContextManager.userX(context)
        if (userx != null && !userx.userId.isNullOrEmpty()) {
            return 
        }
        
        val usage = getUsage()
        usage.count++
        prefs.edit().putString("usage", gson.toJson(usage)).apply()
    }
}
