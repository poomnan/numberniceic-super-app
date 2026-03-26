package com.numberniceic.ui.admin

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.numberniceic.R

class TestGuestAddressActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_guest_address)
        
        val titleText = findViewById<TextView>(R.id.titleText)
        val backButton = findViewById<Button>(R.id.backButton)
        val testApiButton = findViewById<Button>(R.id.testApiButton)
        
        titleText.text = "หน้าทดสอบ Guest Address Management"
        
        backButton.setOnClickListener {
            finish()
        }
        
        testApiButton.setOnClickListener {
            titleText.text = "กำลังทดสอบ API..."
            // TODO: Add API test here
        }
    }
}
