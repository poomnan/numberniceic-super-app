package com.numberniceic.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.numberniceic.R
import com.numberniceic.data.rengyam.LengYamDao
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.SplashActivity
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.concurrent.TimeUnit

class WanpraReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("WanpraWorker", "Starting Wanpra check...")
        
        try {
            val response = RetrofitClient.api.getLengYam().execute()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                checkAndNotify(data)
            }
        } catch (e: Exception) {
            Log.e("WanpraWorker", "Error fetching Wanpra data", e)
            return Result.retry()
        }

        return Result.success()
    }

    private fun checkAndNotify(data: LengYamDao) {
        val today = DateTime.now(org.joda.time.DateTimeZone.forID("Asia/Bangkok")).withTimeAtStartOfDay()
        val tomorrow = today.plusDays(1)
        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")

        Log.d("WanpraWorker", "Checking notifications for Today: $today, Tomorrow: $tomorrow")

        data.wanPras?.forEach { wanpra ->
            val wanDateStr = wanpra.wanpraDate ?: return@forEach
            val wanDate = try { 
                formatter.parseDateTime(wanDateStr).withTimeAtStartOfDay() 
            } catch (e: Exception) { 
                null 
            } ?: return@forEach
            
            // Robust parsing for Any type
            val pWorkerStr = wanpra.isWanpra?.toString()?.lowercase()
            val isP = pWorkerStr == "1" || pWorkerStr == "1.0" || pWorkerStr == "true"
            val tWorkerStr = wanpra.isTongchai?.toString()?.lowercase()
            val isT = tWorkerStr == "1" || tWorkerStr == "1.0" || tWorkerStr == "true"
            val aWorkerStr = wanpra.isAtipbadee?.toString()?.lowercase()
            val isA = aWorkerStr == "1" || aWorkerStr == "1.0" || aWorkerStr == "true"

            val events = mutableListOf<String>()
            if (isP) events.add("วันพระ")
            if (isT) events.add("วันธงชัย")
            if (isA) events.add("วันอธิบดี")

            if (events.isEmpty()) return@forEach

            val dayNum = wanDate.dayOfMonth().asString
            val monthThai = com.numberniceic.utils.PersonContextManager.toThaiMonth(wanDate.monthOfYear().asText)[0]
            val yearThai = wanDate.year + 543
            val fullThaiDate = "$dayNum $monthThai $yearThai"
            
            val eventString = if (events.size == 1) {
                events[0]
            } else {
                val last = events.last()
                val others = events.dropLast(1).joinToString(", ")
                "$others และ$last"
            }

            if (wanDate.isEqual(today)) {
                val body = getSmartBody(isP, isT, isA, true)
                showNotification("วันนี้$eventString ($fullThaiDate)", body, 1001) // Fixed ID for Today
            } else if (wanDate.isEqual(tomorrow)) {
                val body = getSmartBody(isP, isT, isA, false)
                showNotification("พรุ่งนี้$eventString ($fullThaiDate)", body, 1002) // Fixed ID for Tomorrow
            }
        }
    }

    private fun getSmartBody(isP: Boolean, isT: Boolean, isA: Boolean, isToday: Boolean): String {
        val timeWord = if (isToday) "วันนี้" else "วันพรุ่งนี้"
        
        if (isP && isT && isA) {
            return "สุดยอดมหาฤกษ์มงคล! เป็นทั้งวันพระ วันธงชัย และวันอธิบดี เหมาะอย่างยิ่งในการทำบุญและเริ่มงานใหญ่ เพื่อความสำเร็จสูงสุดใน${timeWord}"
        }
        if (isP && isT) {
            return "วันมงคลคูณสอง! ได้ทั้งบุญและชัยชนะ ${timeWord}เหมาะแก่การทำบุญและเริ่มต้นสิ่งใหม่ให้ราบรื่นสำเร็จ"
        }
        if (isP && isA) {
            return "วันดีมีมงคล! เป็นทั้งวันพระและวันอธิบดี ${timeWord}ควรเข้าวัดทำบุญและเจรจาเรื่องสำคัญเพื่อความก้าวหน้า"
        }
        if (isT && isA) {
            return "วันแห่งอำนาจและชัยชนะ! ${timeWord}ฤกษ์ดีมากสำหรับการเปิดกิจการ ตัดสินใจเรื่องใหญ่ หรือการแข่งขันต่างๆ"
        }
        
        if (isP) {
            return if (isToday) "ขอเชิญสะสมบุญ รักษาศีล และเจริญภาวนาเพื่อความเป็นสิริมงคลในวันนี้" 
                   else "เตรียมตัวทำบุญในวันพรุ่งนี้ เพื่อเสริมดวงชะตาและสร้างบารมี"
        }
        if (isT) {
            return "${timeWord}เป็นวันแห่งชัยชนะ ทำการสิ่งใดก็สำเร็จ เหมาะแก่การเริ่มต้นโครงการใหม่ๆ หรือการแข่งขัน"
        }
        if (isA) {
            return "${timeWord}เป็นวันแห่งความเป็นใหญ่ เหมาะแก่การสั่งการ การปกครอง หรือตัดสินใจเรื่องสำคัญ"
        }
        
        return "ขอให้${timeWord}เป็นวันที่ดีของคุณ"
    }

    private fun showNotification(title: String, body: String, notificationId: Int) {
        val channelId = "wanpra_reminders"

        val intent = Intent(applicationContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, notificationId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        createNotificationChannel(channelId)

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Save to Local History (Bell Icon list)
        com.numberniceic.data.local.NotificationStorage.saveNotification(
            applicationContext, title, body, null, "wanpra", null, null
        )

        with(NotificationManagerCompat.from(applicationContext)) {
            try {
                notify(notificationId, builder.build())
                Log.d("WanpraWorker", "Notification sent: $title (ID: $notificationId)")
            } catch (e: SecurityException) {
                Log.e("WanpraWorker", "Permission missing for notification", e)
            }
        }
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "แจ้งเตือนวันพระ"
            val descriptionText = "แจ้งเตือนเมื่อถึงวันพระหรือก่อนวันพระ 1 วัน"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Calculate Initial Delay for 07:00 AM in Asia/Bangkok
            val tz = org.joda.time.DateTimeZone.forID("Asia/Bangkok")
            val currentDate = DateTime.now(tz)
            var dueDate = currentDate.withTimeAtStartOfDay().plusHours(7) // Today at 07:00 AM BKK

            if (dueDate.isBefore(currentDate)) {
                // If already passed 07:00, schedule for tomorrow 07:00
                dueDate = dueDate.plusDays(1)
            }
            
            val delayInMillis = dueDate.millis - currentDate.millis

            val request = PeriodicWorkRequestBuilder<WanpraReminderWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                .build()

            // Use UPDATE to replace the old random schedule with this fixed time
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "WanpraReminderWork",
                ExistingPeriodicWorkPolicy.UPDATE, 
                request
            )
            Log.d("WanpraWorker", "Wanpra reminder scheduled for 07:00 AM (BKK). Delay: " + TimeUnit.MILLISECONDS.toMinutes(delayInMillis) + " mins")
        }
    }
}
