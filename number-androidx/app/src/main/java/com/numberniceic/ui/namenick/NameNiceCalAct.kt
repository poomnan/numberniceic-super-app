package com.numberniceic.ui.namenick

import android.os.Bundle
import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.numberniceic.R

import androidx.appcompat.widget.Toolbar


class NameNiceCalAct : AppCompatActivity() {
    private lateinit var charx: String
    private var day: Int? = null
    private var nickname: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_name_nice_cal)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_namenick_cal))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        this.day = intent.getIntExtra("dayPosition", 0)
        this.nickname = intent.getStringExtra("nickname")

        supportActionBar!!.title = "ถอดรหัสชื่อเล่น $nickname"


    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        supportFragmentManager.beginTransaction().replace(R.id.namenick_cal_container,
                NameNickF.newInstance(this.day!!, this.nickname!!), "NameNickF").commit()




    }






    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}




