package com.numberniceic.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.numberniceic.R

class BuddhaExpirationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val userId = inputData.getString("user_id")
        val expiryType = inputData.getString("expiry_type") ?: "buddha"

        val notificationTitle = "หมดเวลาการแสดงผล"
        val notificationBody = if (expiryType == "inauspicious" || expiryType == "inauspicious_expiry") {
            "รายการ 'วันอัปมงคล และ ทิศอัปมงคลปีนี้' ที่แอดมินแนะนำได้หมดเวลาการแสดงผลแล้ว"
        } else {
            "รายการ 'พระปางประจำปีเกิด' ที่แอดมินแนะนำได้หมดเวลาการแสดงผลแล้ว"
        }

        // 1. Clear Local History (if any)
        /*
        if (expiryType == "buddha" || expiryType == "annual_buddha") {
            clearLocalBuddhaHistory()
        }
        
        // 2. Save to In-App Notification List
        if (userId != null) {
            com.numberniceic.data.local.NotificationStorage.saveNotification(
                context = applicationContext,
                title = notificationTitle,
                body = notificationBody,
                targetUserId = userId,
                type = if (expiryType == "inauspicious") "inauspicious_expiry" else "buddha_expiry"
            )
        }

        // 3. Show System Notification
        showNotification(notificationTitle, notificationBody)
        */
        
        android.util.Log.d("BuddhaExpiration", "Worker fired but logic is DISABLED to prevent premature expiry.")
        return Result.success()
    }

    private fun clearLocalBuddhaHistory() {
        try {
            // Need userId to clear specific history? 
            // Since Worker doesn't know current user easily without inputData,
            // we can either pass userId or clear ALL history prefs matching pattern.
            // For simplicity, we'll assume we pass userId in inputData.
            val userId = inputData.getString("user_id") ?: return
            
            val prefs = applicationContext.getSharedPreferences("buddha_history_$userId", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "buddha_expiry_channel"
        val channelName = "Buddha Expiry"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for Buddha Pang expiration"
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
 
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.buddha) // Use a valid icon name
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            // Use generic try-catch to avoid crash on permission issues
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            notificationManager.notify(999, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
