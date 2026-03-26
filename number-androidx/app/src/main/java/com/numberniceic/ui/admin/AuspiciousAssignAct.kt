package com.numberniceic.ui.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R
import androidx.appcompat.widget.Toolbar

class AuspiciousAssignAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buddha_assign) // Reuse layout

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_buddha_assign))

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "เลือกผู้ใช้เพื่อจัดการวันมงคล"


        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.buddha_assign_container,
                    AuspiciousAssignF.newInstance(), "AuspiciousAssignF").commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
