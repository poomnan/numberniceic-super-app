package com.numberniceic.ui.auth

import android.os.Bundle
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R

class UserLoginAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_login)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_userlogin_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportActionBar!!.title = "สมาชิก Login เพื่อเข้าสู่ระบบ"
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container_userlogin,
                UserLoginF(), "UserLoginF").commit()

        val screen = intent.getStringExtra("sensorLandscape")

        if (screen != null){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)


    }

}
