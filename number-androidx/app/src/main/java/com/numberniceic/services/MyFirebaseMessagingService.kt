package com.numberniceic.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.SplashActivity
import com.numberniceic.utils.UserContextManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Data payload: ${remoteMessage.data}")
        Log.d(TAG, "Notification: ${remoteMessage.notification}")

        // Extract data from either data or notification payload
        var title = remoteMessage.data["title"] 
            ?: remoteMessage.notification?.title 
            ?: "NumberNice"
        var body = remoteMessage.data["body"] 
            ?: remoteMessage.notification?.body 
            ?: ""
        val type = remoteMessage.data["type"] ?: "chat"
        val url = remoteMessage.data["url"]
        val imageUrl = remoteMessage.data["image_url"] ?: remoteMessage.data["imageUrl"]
        val note = remoteMessage.data["note"]
        val targetMemberId = remoteMessage.data["memberid"]
        val sessionId = remoteMessage.data["session_id"] ?: remoteMessage.data["sessionId"]
        
        // 🆕 Improve "meaning" for Chat notifications
        val isChat = type == "chat" || type == "admin_message" || type == "customer_message"
        if (isChat) {
            if (title == "NumberNice" || title.isBlank()) {
                title = "ข้อความจากคุณนิน"
            }
            if (body.isBlank() && !imageUrl.isNullOrBlank()) {
                body = "[รูปภาพ]"
            }
        }
        // Fix: Use multiple keys for memberId to stay synced with Backend (CamelCase, SnakeCase, etc.)
        val memberId = remoteMessage.data["memberid"] 
            ?: remoteMessage.data["memberId"] 
            ?: remoteMessage.data["member_id"] 
            ?: remoteMessage.data["id"]
            
        val currentUser = UserContextManager.userX(this)
        val isAdmin = UserContextManager.isAdmin(this)

        Log.d("FCM_LOG", "Received notification: title=$title, type=$type, memberId=$memberId, sessionId=$sessionId")
        Log.d("FCM_LOG", "Current User ID: ${currentUser?.userId}, isAdmin: $isAdmin")

        // 🛡️ Logic: Check if this notification is intended for the current app context
        var shouldShowPopup = true
        
        if (!memberId.isNullOrBlank()) {
            // A. Targeted at a specific Member ID
            if (currentUser?.userId != memberId) {
                Log.d("FCM_LOG", "🚫 Notification skipped: Targeted at UID $memberId (Current user is ${currentUser?.userId})")
                shouldShowPopup = false
            } else {
                Log.d("FCM_LOG", "✅ Notification matches current user ID ($memberId)")
            }
        } else if (!sessionId.isNullOrBlank()) {
            // B. Targeted at a specific Session ID (Important for Guest Chats)
            val chatPrefs = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
            val currentSessionId = chatPrefs.getString("session_id", "")
            if (currentSessionId != sessionId) {
                Log.d("FCM_LOG", "🚫 Notification skipped: Targeted at SID $sessionId (Current session is $currentSessionId)")
                shouldShowPopup = false
            } else {
                Log.d("FCM_LOG", "✅ Notification matches current session ID ($sessionId)")
            }
        } else {
            // C. General Notification (No memberId/sessionId)
            // 🛡️ Extra Defense: Check if this is an Admin-only notification type
            val isAdminType = type == "customer_message" || type == "new_order"
           
            if (isAdminType && !isAdmin) {
                Log.d("FCM_LOG", "🚫 Notification skipped: Admin type ($type) but current user is not admin")
                shouldShowPopup = false
            } else {
                Log.d("FCM_LOG", "📢 General notification or Admin match, showing to all/admin")
            }
        }
        
        Log.d(TAG, "Parsed - title: $title, body: $body, type: $type")

        // 2. Save to storage AND Show Popup ONLY if notification is for the current user
        if (shouldShowPopup) {
            saveNotificationToDb(title, body, memberId, type, url, note, sessionId)

            val isChatNotif = type == "chat" || type == "admin_message" || type == "customer_message"
            if (isChatNotif && com.numberniceic.utils.ChatNotificationManager.isChatScreenOpen) {
                Log.d(TAG, "Skipping Popup: User is already in ChatScreen")
            } else {
                sendNotification(title, body, type, url, sessionId)
            }
        }
    }

    private fun saveNotificationToDb(title: String, body: String, memberId: String? = null, type: String? = null, url: String? = null, note: String? = null, sessionId: String? = null) {
        // 🚀 Fix: Chat messages should NOT be saved to the general notification list (Bell Icon List) here.
        // The AppActivity polling logic creates a dedicated "Bubble" entry for them.
        // Saving here would create a duplicate "Bell" entry.
        val isChat = type == "chat" || type == "admin_message" || type == "customer_message"
        
        if (!isChat) {
            com.numberniceic.data.local.NotificationStorage.saveNotification(applicationContext, title, body, memberId, type, url, note)
        }
        
        // Broadcast to update UI (Current User only) - WE STILL NEED THIS for Sound & Badge updates
        val currentUser = UserContextManager.userX(applicationContext)
        val isAdmin = UserContextManager.isAdmin(applicationContext)
        
        // Final sanity check for UI updates
        var isForThisUser = false
        if (!memberId.isNullOrBlank()) {
            isForThisUser = currentUser?.userId == memberId
        } else if (!sessionId.isNullOrBlank()) {
            val chatPrefs = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
            val currentSessionId = chatPrefs.getString("session_id", "")
            isForThisUser = currentSessionId == sessionId
        } else {
            // General or Role-based
            val isAdminType = type == "customer_message" || type == "new_order"
            isForThisUser = if (isAdminType) isAdmin else true
        }

        if (isForThisUser) {
            val intent = Intent("com.numberniceic.NEW_NOTIFICATION")
            intent.putExtra("title", title)
            intent.putExtra("body", body)
            intent.putExtra("type", type)
            intent.putExtra("url", url)
            intent.putExtra("note", note)
            // 🚀 Add session_id so Chat Screens can filter/refresh specifically
            intent.putExtra("session_id", sessionId) 
            sendBroadcast(intent)
        } else {
            Log.d(TAG, "Skipping UI Broadcast: Not for this user/session/role")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun handleNow() {
        Log.d(TAG, "Short lived task is done.")
    }

    private fun sendRegistrationToServer(token: String) {
        // 🔔 ALWAYS save token locally first (for guest users)
        val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
        Log.d(TAG, "FCM token saved locally: ${token.take(10)}...")
        
        val user = UserContextManager.userX(this)
        if (user != null && user.userId != null) {
            val json = JsonObject()
            json.addProperty("memberid", user.userId)
            json.addProperty("token", token)

            try {
                val apiService = RetrofitClient.instance.create(ApiService::class.java)
                apiService.updateFcmToken(json).enqueue(object : Callback<JsonObject> {
                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                         if (response.isSuccessful) {
                            Log.d(TAG, "Token updated successfully for user ${user.userId}")
                        } else {
                            Log.e(TAG, "Failed to update token: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                         Log.e(TAG, "Failed to update token: ${t.message}")
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Retrofit: ${e.message}")
            }

        } else {
             Log.d(TAG, "Guest user - token saved locally for chat notifications")
        }
    }

    private fun sendNotification(title: String, messageBody: String, type: String? = null, url: String? = null, sessionId: String? = null) {
        Log.d(TAG, "sendNotification: title=$title, body=$messageBody, type=$type, url=$url")
        
        // 🚀 Target AppActivity directly to avoid Splash delay (Restart feel)
        val intent = Intent(this, com.numberniceic.ui.AppActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra("title", title)
        intent.putExtra("body", messageBody)
        intent.putExtra("type", type)
        intent.putExtra("url", url)
        if (sessionId != null) intent.putExtra("session_id", sessionId)
        
        // 🆕 Chat Notification - เปิดหน้า ChatScreen
        val isChatNotification = type == "chat" || type == "admin_message" || type == "customer_message"
        if (isChatNotification) {
            if (type == "customer_message") {
                intent.putExtra("open_admin_chat", true)
                Log.d(TAG, "Added open_admin_chat=true and session_id=$sessionId to intent")
            } else {
                intent.putExtra("open_chat", true)
                Log.d(TAG, "Added open_chat=true and session_id=$sessionId to intent")
            }
        }
        
        // 🆕 New Order Notification - เปิดหน้าออเดอร์เพทาย (Zircon)
        if (type == "new_order") {
            intent.putExtra("open_zircon_orders", true)
            Log.d(TAG, "Added open_zircon_orders=true to intent")
        }
        
        val isBagColor = title.contains("สีกระเป๋า") || messageBody.contains("สีกระเป๋า")
        Log.d(TAG, "isBagColor: $isBagColor")
        
        if (isBagColor) {
            intent.putExtra("open_dashboard", true)
            Log.d(TAG, "Added open_dashboard=true to intent")
        }

        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // 🆕 นับจำนวนข้อความที่ยังไม่ได้อ่าน
        val unreadCount = getUnreadChatCount()
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        // 🆕 แสดง badge number สำหรับ chat
        if (isChatNotification && unreadCount > 0) {
            notificationBuilder.setNumber(unreadCount)
            notificationBuilder.setSubText("$unreadCount ข้อความใหม่")
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "NumberNice Notifications",
                NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifications from NumberNice"
            notificationManager.createNotificationChannel(channel)
        }

        // 🆕 ใช้ notification ID แยกตาม type
        val notificationId = if (isChatNotification) 1001 else 0
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        // 🆕 บันทึก unread count
        if (isChatNotification) {
            incrementUnreadChatCount()
        }
    }
    
    // 🆕 ฟังก์ชันนับข้อความที่ยังไม่ได้อ่าน
    private fun getUnreadChatCount(): Int {
        val prefs = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("unread_count", 0)
    }
    
    private fun incrementUnreadChatCount() {
        val prefs = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("unread_count", 0)
        prefs.edit().putInt("unread_count", currentCount + 1).apply()
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
