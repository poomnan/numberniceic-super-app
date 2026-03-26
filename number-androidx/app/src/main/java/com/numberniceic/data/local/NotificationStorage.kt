package com.numberniceic.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.numberniceic.utils.UserContextManager

data class NotiModel(
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val date: Long = 0L,
    val dateDisplay: String = "",
    var isRead: Boolean = false,
    val type: String? = null,
    val url: String? = null,
    val note: String? = null
)

object NotificationStorage {
    private const val PREF_NAME = "noti_prefs"
    private const val KEY_LIST = "noti_list"
    private const val KEY_GLOBAL = "noti_list_global"
    private const val TAG = "NotificationStorage"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getUserKey(context: Context, userId: String? = null): String {
        val id = if (!userId.isNullOrEmpty()) {
            userId.trim()
        } else {
            com.numberniceic.utils.UserContextManager.userX(context)?.userId?.trim()
        }
        
        val key = if (!id.isNullOrEmpty()) "${KEY_LIST}_$id" else KEY_GLOBAL
        Log.d(TAG, "getUserKey: userIdInput=$userId, finalKey=$key")
        return key
    }

    fun saveNotification(context: Context, title: String, body: String, targetUserId: String? = null, type: String? = null, url: String? = null, note: String? = null) {
        val key = getUserKey(context, targetUserId)
        
        val jsonOld = getPrefs(context).getString(key, null)
        val listType = object : TypeToken<List<NotiModel>>() {}.type
        
        val list: MutableList<NotiModel> = if (!jsonOld.isNullOrEmpty()) {
            try {
                GsonBuilder().setLenient().create().fromJson(jsonOld, listType) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing notification list for key $key: ${e.message}")
                // Fallback to empty list to prevent crash, but log it.
                // In production, consider backing up corrupted data.
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        val dateDisplay = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        
        // Dedup check (last 10 seconds)
        val isDuplicate = list.take(5).any { 
            it.title == title && it.body == body && (System.currentTimeMillis() - it.date < 10000) 
        }
        if (isDuplicate) {
            Log.d(TAG, "Duplicate notification detected, skipping save.")
            return
        }

        // Smart Type Inference if type is null or "custom"
        var finalType = type
        if (finalType == null || finalType == "custom" || finalType == "topic_custom") {
            val content = (title + " " + body).lowercase()
            finalType = when {
                content.contains("ทำบุญ") || content.contains("สะสมบุญ") || content.contains("วิธีทำ") -> "webview_merit"
                content.contains("เปลี่ยนเบอร์") || content.contains("เปลี่ยนเลข") || content.contains("แนวทางแก้ไข") -> "webview_changenum"
                content.contains("คาถา") || content.contains("บทสว") || content.contains("คำเตือน") -> "webview_spell"
                else -> finalType // Keep as custom if no match
            }
            Log.d(TAG, "Smart Inference: type updated from $type to $finalType")
        }

        list.add(0, NotiModel(0, title, body, System.currentTimeMillis(), dateDisplay, false, finalType, url, note))
        
        if (list.size > 50) {
            list.removeAt(list.size - 1)
        }
        
        val json = Gson().toJson(list)
        getPrefs(context).edit().putString(key, json).apply()
        Log.d(TAG, "Saved notification to $key. New size: ${list.size}")
    }

    fun getNotifications(context: Context): List<NotiModel> {
        val userKey = getUserKey(context)
        migrateOldKeys(context)
        
        val listType = object : TypeToken<List<NotiModel>>() {}.type
        val gson = GsonBuilder().setLenient().create()
        
        // Load User Notifications
        val userJson = getPrefs(context).getString(userKey, null)
        val userList: MutableList<NotiModel> = if (!userJson.isNullOrEmpty()) {
            try {
                gson.fromJson(userJson, listType) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing user notifications ($userKey): ${e.message}")
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        // Load Global Notifications if current is not global
        val result = if (userKey != KEY_GLOBAL) {
            val globalJson = getPrefs(context).getString(KEY_GLOBAL, null)
            val globalList: List<NotiModel> = if (!globalJson.isNullOrEmpty()) {
                try {
                    gson.fromJson(globalJson, listType) ?: emptyList()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing global notifications: ${e.message}")
                    emptyList()
                }
            } else {
                emptyList()
            }
            // Merge: User list takes precedence (shadowing global items if duplicated)
            (userList + globalList).distinctBy { "${it.title}|${it.body}|${it.date}" }.sortedByDescending { it.date }
        } else {
            userList.sortedByDescending { it.date }
        }
        
        // Filter out blacklisted items
        return result.filter { !isBlacklisted(context, getBlacklistKey(it)) }
    }

    private fun migrateOldKeys(context: Context) {
        val prefs = getPrefs(context)
        
        if (prefs.contains(KEY_LIST)) {
            val oldJson = prefs.getString(KEY_LIST, null)
            if (oldJson != null) {
                val globalJson = prefs.getString(KEY_GLOBAL, null)
                val listType = object : TypeToken<MutableList<NotiModel>>() {}.type
                val gson = GsonBuilder().setLenient().create()
                
                val oldList: MutableList<NotiModel> = try { gson.fromJson(oldJson, listType) } catch (e: Exception) { mutableListOf() }
                val currentGlobal: MutableList<NotiModel> = try { gson.fromJson(globalJson, listType) } catch (e: Exception) { mutableListOf() }
                
                val merged = (currentGlobal + oldList).distinctBy { "${it.title}|${it.body}|${it.date}" }.sortedByDescending { it.date }
                prefs.edit().putString(KEY_GLOBAL, Gson().toJson(merged)).remove(KEY_LIST).apply()
                Log.d(TAG, "Migrated old KEY_LIST to KEY_GLOBAL")
            }
        }
    }

    fun getLatestByType(context: Context, typeStr: String): NotiModel? {
        return getNotifications(context).firstOrNull { it.type == typeStr }
    }

    fun getAllByType(context: Context, typeStr: String): List<NotiModel> {
        return getNotifications(context).filter { it.type == typeStr }
    }

    fun getUnreadCount(context: Context): Int {
        // Exclude chat notifications from the global bell count
        return getNotifications(context).count { 
            !it.isRead && it.type != "chat" && it.type != "admin_message"
        }
    }

    fun markAllRead(context: Context) {
        val userKey = getUserKey(context)
        val updatedList = getNotifications(context).map { 
            it.copy(isRead = true)
        }
        val json = Gson().toJson(updatedList)
        getPrefs(context).edit().putString(userKey, json).apply()
    }
    
    fun markReadByContent(context: Context, title: String, body: String): Boolean {
        val key = getUserKey(context)
        val list = getNotifications(context).toMutableList()
        var updated = false
        
        val iterator = list.listIterator()
        val targetTitle = title.trim()
        val targetBody = body.trim()
        
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (!item.isRead && 
                item.title.trim().equals(targetTitle, ignoreCase = true) && 
                item.body.trim().equals(targetBody, ignoreCase = true)) {
                iterator.set(item.copy(isRead = true))
                updated = true
            }
        }
        
        if (updated) {
            val json = Gson().toJson(list)
            // Use commit() to ensure data is written to memory/disk immediately before any broadcasts
            getPrefs(context).edit().putString(key, json).commit()
            Log.d(TAG, "markReadByContent: Marked ${title.take(20)}... as read. Saved to $key")
        } else {
             Log.d(TAG, "markReadByContent: Item not found or already read: ${title.take(20)}...")
        }
        return updated
    }

    fun markReadByType(context: Context, typeStr: String): Boolean {
        val key = getUserKey(context)
        val list = getNotifications(context).toMutableList()
        var updated = false
        
        val iterator = list.listIterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (!item.isRead && item.type == typeStr) {
                iterator.set(item.copy(isRead = true))
                updated = true
            }
        }
        
        if (updated) {
            val json = Gson().toJson(list)
            getPrefs(context).edit().putString(key, json).apply()
        }
        return updated
    }

    // --- Blacklist / Deletion Logic ---

    private fun getBlacklistKey(item: NotiModel): String {
        // Create a unique signature for the notification
        return "${item.id}|${item.title}|${item.body}|${item.date}"
    }

    fun deleteNotification(context: Context, noti: NotiModel): Boolean {
        // 1. Add to blacklist (Persistent Delete)
        addToBlacklist(context, getBlacklistKey(noti))
        
        // 2. Remove from local storage (Legacy cleanup)
        val key = getUserKey(context)
        val list = getNotifications(context).toMutableList()
        val removed = list.removeAll { 
            val itKey = getBlacklistKey(it)
            val targetKey = getBlacklistKey(noti)
            itKey == targetKey
        }
        
        // Save the list sans the deleted item (for local user list)
        // Note: Global items will be filtered out by getNotifications via blacklist
        val json = Gson().toJson(list)
        getPrefs(context).edit().putString(key, json).apply()
        
        return true
    }

    fun deleteNotificationAt(context: Context, position: Int) {
        val list = getNotifications(context) // Gets the full visible list
        if (position in 0 until list.size) {
            val itemToDelete = list[position]
            deleteNotification(context, itemToDelete)
        }
    }

    fun clearAll(context: Context) {
        val key = getUserKey(context)
        getPrefs(context).edit().remove(key).apply()
        // Note: This only clears user-local items. 
        // To clear "Global" items from view, we'd need to blacklist them all, 
        // but "Clear All" usually implies clearing user inbox. 
        // Ideally, we should iterate and blacklist all currently visible items.
        val visible = getNotifications(context)
        visible.forEach { addToBlacklist(context, getBlacklistKey(it)) }
    }

    // Server Item Blacklist (for swiped server notifications)
    private const val KEY_BLACKLIST = "noti_blacklist_v2"

    private fun addToBlacklist(context: Context, keyStr: String) {
        val id = com.numberniceic.utils.UserContextManager.userX(context)?.userId ?: "guest"
        val prefKey = "${KEY_BLACKLIST}_$id"
        val prefs = getPrefs(context)
        val blacklisted = getBlacklist(context).toMutableSet()
        blacklisted.add(keyStr)
        prefs.edit().putStringSet(prefKey, blacklisted).apply()
        Log.d(TAG, "Blacklisted item: $keyStr")
    }

    fun isBlacklisted(context: Context, keyStr: String): Boolean {
        return getBlacklist(context).contains(keyStr)
    }
    
    // Legacy support for ID-based checking
    fun isBlacklistedId(context: Context, serverId: Long): Boolean {
        if (serverId == 0L) return false
        // This is a rough check, mainly for strict server-id based systems
         return getBlacklist(context).any { it.startsWith("$serverId|") }
    }

    private fun getBlacklist(context: Context): Set<String> {
        val id = com.numberniceic.utils.UserContextManager.userX(context)?.userId ?: "guest"
        val prefKey = "${KEY_BLACKLIST}_$id"
        return getPrefs(context).getStringSet(prefKey, emptySet()) ?: emptySet()
    }
}
