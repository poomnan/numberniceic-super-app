package com.numberniceic.ui.bag

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.numberniceic.R

/**
 * Activity สำหรับแสดง Demo BagColorView
 */
class BagColorDemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bag_color_demo)

        // Set up toolbar
        supportActionBar?.apply {
            title = "สีกระเป๋ามงคล"
            setDisplayHomeAsUpEnabled(true)
        }

        // Load fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, BagColorDemoFragment.newInstance())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
