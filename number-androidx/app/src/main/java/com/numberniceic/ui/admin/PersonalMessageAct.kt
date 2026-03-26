package com.numberniceic.ui.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R
import androidx.appcompat.widget.Toolbar

class PersonalMessageAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_message)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_personal_msg_act))

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "ส่งข้อความพิเศษ"


        supportFragmentManager.beginTransaction().replace(R.id.personal_msg_container,
                PersonalMessageF.newInstance(), "PersonalMessageF").commit()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
