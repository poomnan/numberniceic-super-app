package com.numberniceic.ui.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R
import androidx.appcompat.widget.Toolbar

class LuckyNumberAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lucky_number)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_lucky_act))

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "เพิ่มรายการ Lucky Number"


        supportFragmentManager.beginTransaction().replace(R.id.luckynumber_container,
                LuckyNumberF(), "LuckyNumberF").commit()


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}