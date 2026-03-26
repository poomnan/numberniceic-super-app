package com.numberniceic.ui.news

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R

class NewsAllAct : AppCompatActivity() {

    private lateinit var viewModel: com.numberniceic.ui.news.NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_all)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_news_all_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val newsIdType = intent.getStringExtra("newsIdType") ?: "1"

        supportFragmentManager.beginTransaction().replace(R.id.news_all_container,
                NewsAllF.newInstance(newsIdType), "NewsAllF").commit()

        val title = when (newsIdType) {
            "0" -> "ข่าวและบทความที่น่าสนใจ"
            "1" -> "Review จากลูกค้า"
            "2" -> "วิธีเลือกซื้อเบอร์โทรศัพท์มงคล"
            "3" -> "ทำนายดวงชะตาจากชื่อ-สกุล"
            "4" -> "ทำนายดวงชะตาจากทะเบียนรถ"
            "5" -> "ทำนายดวงชะตาจากบ้านเลขที่"
            "6" -> "หลักการใช้เลขมงคล"
            else -> ""
        }
        supportActionBar!!.title = title

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}