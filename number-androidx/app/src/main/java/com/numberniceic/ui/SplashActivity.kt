package com.numberniceic.ui

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

import com.numberniceic.R

import java.lang.Exception
import android.os.Handler

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        android.util.Log.d("SplashActivity", "onCreate called")
        android.util.Log.d("SplashActivity", "open_dashboard extra: ${intent.getBooleanExtra("open_dashboard", false)}")

        Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(baseContext, AppActivity::class.java)
                if (getIntent().extras != null) {
                    intent.putExtras(getIntent().extras!!)
                    android.util.Log.d("SplashActivity", "Passed extras to AppActivity")
                }
                startActivity(intent)
                overridePendingTransition(R.anim.fade_id, R.anim.fade_out)
                finish() // Good practice to finish splash
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

}
