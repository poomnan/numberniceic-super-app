package com.numberniceic.ui.apersonnews

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R
import com.numberniceic.utils.UserContextManager

class BirthDayAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_birth_day)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_birthday_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        val userx = UserContextManager.userX(applicationContext)
        if (userx != null){
            supportActionBar!!.title = "เพิ่มวันเดือนปีเกิด คุณ${userx.realName}"

        }

        supportFragmentManager.beginTransaction().replace(R.id.birth_day_container,
                BirthDayF(), "BirthDayF").commit()
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }



}
