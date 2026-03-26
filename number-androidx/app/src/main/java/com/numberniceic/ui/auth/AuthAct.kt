package com.numberniceic.ui.auth

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R

class AuthAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_auth_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportActionBar!!.title = "ผู้ใช้งาน VIP CODE"
        supportFragmentManager.beginTransaction().replace(R.id.authActContainer,
                AuthF(), "AuthF").commit()

        val screen = intent.getStringExtra("sensorLandscape")

        if (screen != null){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

    }


    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)




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
