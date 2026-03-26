package com.numberniceic.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.numberniceic.ui.screens.ChatScreen

class ChatComposeActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialMessage = intent.getStringExtra("initial_message")
        setContent {
            ChatScreen(onBack = { finish() }, initialMessage = initialMessage)
        }
    }
}
