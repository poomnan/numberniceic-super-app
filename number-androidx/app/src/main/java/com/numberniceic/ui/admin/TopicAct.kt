package com.numberniceic.ui.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.core.provider.FontRequest
import com.numberniceic.R
import androidx.appcompat.widget.Toolbar

class TopicAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)





        setContentView(R.layout.activity_topic)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_topic_act))

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "เพิ่มบทความ"


        supportFragmentManager.beginTransaction().replace(R.id.topic_container,
                TopicF.newInstance(), "TopicF").commit()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
