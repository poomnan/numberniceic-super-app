package com.numberniceic.ui.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.numberniceic.ui.screens.PhoneCalScreen

class PhoneCalAct : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val phoneNumber = intent.getStringExtra("PHONENUMBER") ?: ""
        val phonePrice = intent.getStringExtra("PHONEPRICE") ?: "0"
        
        setContent {
            MaterialTheme {
                Surface {
                    PhoneCalScreen(
                        initialPhoneNumber = phoneNumber,
                        initialPrice = phonePrice,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

