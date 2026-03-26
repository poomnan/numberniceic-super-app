package com.numberniceic.ui.apersonnews

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R

class DashboardActivity : AppCompatActivity() {

    private val refreshReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            android.util.Log.d("DashboardActivity", "Notification Received - Recreating Dashboard")
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_dashboard_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "หน้าหลัก"

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_dashboard, PersonNewsF(), "PersonNewsF")
                .commit()
        }

        // Register Receiver
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            registerReceiver(refreshReceiver, android.content.IntentFilter("com.numberniceic.NEW_NOTIFICATION"), android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, android.content.IntentFilter("com.numberniceic.NEW_NOTIFICATION"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (e: Exception) { }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
