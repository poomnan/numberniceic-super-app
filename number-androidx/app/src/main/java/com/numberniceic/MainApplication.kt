package com.numberniceic

import android.app.Application
import android.util.Log
import net.danlew.android.joda.JodaTimeAndroid
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.GuestManager

class MainApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        JodaTimeAndroid.init(this)
        
        // Initialize Guest Manager for guest users
        try {
            val guestManager = GuestManager(this)
            val guestId = guestManager.initializeGuestIfNeeded()
            Log.d("MainApplication", "Guest initialized with ID: $guestId")
            
            // ทดสอบระบบ guest (สำหรับ debug)
            if (BuildConfig.DEBUG) {
                Log.d("MainApplication", "DEBUG: Testing guest system...")
                // สามารถเรียก test ได้ที่นี่ถ้าต้องการ
            }
        } catch (e: Exception) {
            Log.e("MainApplication", "Failed to initialize guest manager", e)
        }
        
        // Schedule Wanpra Reminders
        com.numberniceic.workers.WanpraReminderWorker.schedule(this)

    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(RetrofitClient.okHttpClient)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(coil.decode.ImageDecoderDecoder.Factory())
                } else {
                    add(coil.decode.GifDecoder.Factory())
                }
            }
            .build()
    }
}