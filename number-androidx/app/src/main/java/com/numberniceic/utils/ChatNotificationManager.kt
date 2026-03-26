package com.numberniceic.utils

import android.content.Context

/**
 * จัดการ unread chat count สำหรับ notification badge
 */
object ChatNotificationManager {
    
    private const val PREFS_NAME = "chat_prefs"
    private const val KEY_UNREAD_COUNT = "unread_count"
    
    // 🛡️ Global state to track if chat is being actively viewed
    // This allows us to silence notifications when the user is already looking at the screen.
    var isChatScreenOpen: Boolean = false
    
    // 🎯 For Admins: Track which SPECIFIC session is open to only silence THAT one.
    var activeSessionId: String? = null
    
    /**
     * เพิ่มจำนวนข้อความที่ยังไม่ได้อ่าน
     */
    fun incrementUnreadCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(KEY_UNREAD_COUNT, 0)
        prefs.edit().putInt(KEY_UNREAD_COUNT, currentCount + 1).apply()
    }
    
    /**
     * รีเซ็ตจำนวนข้อความที่ยังไม่ได้อ่าน (เมื่อเปิดหน้า chat)
     */
    fun clearUnreadCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_UNREAD_COUNT, 0).apply()
        
        // 🆕 Mark chat notifications in general inbox as read too
        com.numberniceic.data.local.NotificationStorage.markReadByType(context, "chat")
        com.numberniceic.data.local.NotificationStorage.markReadByType(context, "admin_message")
        
        // 🚀 Notify UI to update total badge (Bell icon)
        context.sendBroadcast(android.content.Intent("com.numberniceic.NEW_NOTIFICATION"))
    }
    
    private const val KEY_LAST_READ_ID = "last_read_msg_id"

    /**
     * บันทึก ID ของข้อความล่าสุดที่อ่านแล้ว
     */
    fun setLastReadMessageId(context: Context, messageId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getLastReadMessageId(context)
        if (messageId > current) {
            prefs.edit().putLong(KEY_LAST_READ_ID, messageId).apply()
        }
    }

    fun getLastReadMessageId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            prefs.getLong(KEY_LAST_READ_ID, 0L)
        } catch (e: ClassCastException) {
            // Migration: Value might be stored as Int from previous version bug
            try {
                val oldVal = prefs.getInt(KEY_LAST_READ_ID, 0)
                val fixedVal = oldVal.toLong()
                // Fix it immediately
                prefs.edit().putLong(KEY_LAST_READ_ID, fixedVal).apply()
                fixedVal
            } catch (e2: Exception) {
                0L
            }
        }
    }

    /**
     * ตั้งค่าจำนวน Unread โดยตรง (จากการคำนวณ)
     */
    fun setUnreadCount(context: Context, count: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_UNREAD_COUNT, count).apply()
    }

    /**
     * ดึงจำนวนข้อความที่ยังไม่ได้อ่าน
     */
    fun getUnreadCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_UNREAD_COUNT, 0)
    }
}
