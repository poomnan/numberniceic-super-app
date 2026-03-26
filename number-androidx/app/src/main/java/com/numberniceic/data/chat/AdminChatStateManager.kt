package com.numberniceic.data.chat

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.util.Log

/**
 * Robust manager for tracking which messages have been read by the admin.
 * Uses persistent storage (SharedPreferences) to ensure state survives app restarts.
 */
object AdminChatStateManager {
    // 🆕 Simple Text Persistence: "sessionId:messageId" per line
    private const val FILE_NAME = "admin_read_status_simple.txt"
    private const val PREFS_CACHE = "admin_chat_v4_cache"
    private const val KEY_SESSIONS_CACHE = "sessions_cache"
    
    // 🙈 Hidden Sessions Persistence
    private const val HIDDEN_FILE_NAME = "admin_hidden_sessions_v2.txt"
    
    // 🚀 Reactive State Map: Directly observable by Compose UI
    private val readMessageMap = mutableStateMapOf<String, Long>()
    private val hiddenMessageMap = mutableStateMapOf<String, Long>()
    
    var refreshSignal by mutableStateOf(0)
    
    private val gson = com.google.gson.Gson()

    /**
     * Loads saved read states and hidden sessions.
     */
    fun init(context: android.content.Context) {
        try {
            // Load Read Status
            val file = java.io.File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                val lines = file.readLines()
                readMessageMap.clear()
                var count = 0
                lines.forEach { line ->
                    val lastIdx = line.lastIndexOf(":")
                    if (lastIdx != -1) {
                        val key = line.substring(0, lastIdx)
                        val valueStr = line.substring(lastIdx + 1)
                        val value = valueStr.toLongOrNull() ?: -1L
                        if (value != -1L) {
                            readMessageMap[key] = value
                            count++
                        }
                    }
                }
                Log.d("AdminChatState", "Init: Loaded $count read statuses")
            }

            // Load Hidden Sessions (V2)
            val hiddenFile = java.io.File(context.filesDir, HIDDEN_FILE_NAME)
            if (hiddenFile.exists()) {
                hiddenMessageMap.clear()
                hiddenFile.readLines().forEach { line ->
                    val lastIdx = line.lastIndexOf(":")
                    if (lastIdx != -1) {
                        val key = line.substring(0, lastIdx)
                        val value = line.substring(lastIdx + 1).toLongOrNull() ?: -1L
                        if (value != -1L) hiddenMessageMap[key] = value
                    }
                }
                Log.d("AdminChatState", "Init: Loaded ${hiddenMessageMap.size} hidden sessions")
            }
            
        } catch (e: Exception) {
            Log.e("AdminChatState", "Init Error: ${e.message}")
        }
    }

    /**
     * Hides a session locally AND attempts to delete the message from the server.
     */
    fun hideSession(context: android.content.Context, sessionId: String, messageId: Long) {
        val safeId = sessionId.trim()
        hiddenMessageMap[safeId] = messageId
        try {
            val file = java.io.File(context.filesDir, HIDDEN_FILE_NAME)
            val content = StringBuilder()
            hiddenMessageMap.forEach { (key, value) ->
                content.append("$key:$value\n")
            }
            file.writeText(content.toString())
            Log.d("AdminChatState", "Hidden session $safeId up to $messageId")
        } catch (e: Exception) {
            Log.e("AdminChatState", "Hide Error: ${e.message}")
        }
        
        /* 🚀 COMMENTED OUT: We want "Delete" to be a local hide so it can be "Restored" 
           Server-side deletion is permanent and prevents "Restore All" from working.
        if (messageId != null) {
            deleteMessageFromServer(messageId)
        }
        */
    }

    private fun deleteMessageFromServer(messageId: Long) {
        com.numberniceic.https.RetrofitClient.api.deleteMessage(messageId).enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
            override fun onResponse(call: retrofit2.Call<com.google.gson.JsonObject>, response: retrofit2.Response<com.google.gson.JsonObject>) {
                if (response.isSuccessful) {
                    Log.d("AdminChatState", "Deleted message $messageId from DB successfully")
                } else {
                    Log.e("AdminChatState", "Failed to delete message $messageId: ${response.code()}")
                }
            }
            override fun onFailure(call: retrofit2.Call<com.google.gson.JsonObject>, t: Throwable) {
                Log.e("AdminChatState", "Delete network error: ${t.message}")
            }
        })
    }

    fun clearHiddenSessions(context: android.content.Context) {
        hiddenMessageMap.clear()
        try {
            val file = java.io.File(context.filesDir, HIDDEN_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) { }
        refreshSignal++
    }

    fun isHidden(sessionId: String, latestMsgId: Long): Boolean {
        val hiddenUntil = hiddenMessageMap[sessionId.trim()] ?: -1L
        return latestMsgId <= hiddenUntil
    }

    private fun saveToDisk(context: android.content.Context) {
        try {
            val file = java.io.File(context.filesDir, FILE_NAME)
            val content = StringBuilder()
            readMessageMap.forEach { (key, value) ->
                content.append("$key:$value\n")
            }
            file.writeText(content.toString())
        } catch (e: Exception) {
            Log.e("AdminChatState", "Save Error: ${e.message}")
        }
    }

    /**
     * Marks a message as read for a session. Updates both the reactive map and persistent storage.
     */
    /**
     * Marks a message as read for a session. Updates both the reactive map and persistent storage.
     * ALSO syncs with the server to ensure persistence across devices/installs.
     */
    fun markAsReadLocally(context: android.content.Context, sessionId: String, messageId: Long) {
        val currentMax = readMessageMap[sessionId] ?: -1L
        if (messageId > currentMax) {
            readMessageMap[sessionId] = messageId
            // 🚀 PERSIST IMMEDIATELY
            saveToDisk(context)
            refreshSignal++
            
            // 🆕 Mark chat notifications in general inbox as read too
            com.numberniceic.data.local.NotificationStorage.markReadByType(context, "chat")
            com.numberniceic.data.local.NotificationStorage.markReadByType(context, "admin_message")
            
            // 🚀 Notify UI to update total badge immediately
            context.sendBroadcast(android.content.Intent("com.numberniceic.NEW_NOTIFICATION"))
            
            // 🚀 SYNC WITH SERVER
            com.numberniceic.https.RetrofitClient.api.markChatAsReadAdmin(sessionId).enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
                 override fun onResponse(call: retrofit2.Call<com.google.gson.JsonObject>, response: retrofit2.Response<com.google.gson.JsonObject>) {
                     if (!response.isSuccessful) {
                         Log.e("AdminChatState", "Failed to mark read on server: ${response.code()}")
                     } else {
                         Log.d("AdminChatState", "Marked read on server for session: $sessionId")
                     }
                 }
                 override fun onFailure(call: retrofit2.Call<com.google.gson.JsonObject>, t: Throwable) {
                     Log.e("AdminChatState", "Mark read network error: ${t.message}")
                 }
            })
        }
    }

    /**
     * Force mark all provided sessions as read (Local Override).
     */
    fun markAllRead(context: android.content.Context, sessions: List<ChatMessage>) {
        var changed = false
        sessions.forEach { msg ->
            val currentLocal = readMessageMap[msg.sessionId] ?: -1L
            if (msg.messageId > currentLocal) {
                readMessageMap[msg.sessionId] = msg.messageId
                changed = true
            }
        }
        if (changed) {
            saveToDisk(context)
            refreshSignal++
            
            // 🆕 Mark chat notifications in general inbox as read too
            com.numberniceic.data.local.NotificationStorage.markReadByType(context, "chat")
            com.numberniceic.data.local.NotificationStorage.markReadByType(context, "admin_message")
            
            // 🚀 Notify UI to update total badge
            context.sendBroadcast(android.content.Intent("com.numberniceic.NEW_NOTIFICATION"))
            Log.d("AdminChatState", "Mass Marked sessions as read via JSON.")
        }
    }

    /**
     * Decision maker for red dot visibility. Checks server status against absolute local persistent truth.
     */
    fun isMessageRead(sessionId: String, messageId: Long, serverSideRead: Boolean): Boolean {
        if (serverSideRead) return true
        val localMax = readMessageMap[sessionId] ?: -1L
        val isRead = messageId <= localMax
        // Log.d("AdminChatDebug", "CHECK: $sessionId | MsgId: $messageId | LocalMax: $localMax | ServerRead: $serverSideRead | RESULT: $isRead")
        if (!isRead && localMax != -1L) {
             Log.e("AdminChatDebug", "RED DOT SHOWING: Session=$sessionId, ID=$messageId > Local=$localMax")
        }
        return isRead
    }

    /* --- Session List Caching --- */

    fun saveSessionsCache(context: android.content.Context, sessions: List<ChatMessage>) {
        try {
            val json = gson.toJson(sessions)
            context.getSharedPreferences(PREFS_CACHE, android.content.Context.MODE_PRIVATE)
                .edit().putString(KEY_SESSIONS_CACHE, json).apply()
                
            // 🚀 Self-Learning Sync
            var changed = false
            sessions.forEach { msg ->
                // If server says read, trust it and update local max
                if (msg.isRead) {
                    val currentLocal = readMessageMap[msg.sessionId] ?: -1L
                    if (msg.messageId > currentLocal) {
                        readMessageMap[msg.sessionId] = msg.messageId
                        changed = true
                    }
                }
            }
            if (changed) {
                saveToDisk(context)
            }
        } catch (e: Exception) {
            Log.e("AdminChatState", "Cache Save Error: ${e.message}")
        }
    }

    fun getSessionsCache(context: android.content.Context): List<ChatMessage> {
        return try {
            val json = context.getSharedPreferences(PREFS_CACHE, android.content.Context.MODE_PRIVATE)
                .getString(KEY_SESSIONS_CACHE, null) ?: return emptyList()
            val itemType = object : com.google.gson.reflect.TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson<List<ChatMessage>>(json, itemType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear(context: android.content.Context) {
        readMessageMap.clear()
        try {
            val file = java.io.File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) { }
        
        context.getSharedPreferences(PREFS_CACHE, android.content.Context.MODE_PRIVATE).edit().clear().apply()
        refreshSignal++
    }
}
