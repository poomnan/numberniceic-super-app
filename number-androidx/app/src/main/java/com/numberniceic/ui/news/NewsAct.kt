package com.numberniceic.ui.news

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R

class NewsAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_news_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val newsId = intent.getStringExtra("newsId")
        val newsHeadline = intent.getParcelableExtra<com.numberniceic.data.news.NewsHeadline>("newsObject")

        supportFragmentManager.beginTransaction().replace(R.id.news_container,
                NewsDetailF.newInstance(newsId, newsHeadline), "NewsDetailF").commit()


        supportActionBar!!.title = "บทความข่าว"

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}
