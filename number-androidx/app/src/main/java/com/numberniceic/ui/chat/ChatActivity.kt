package com.numberniceic.ui.chat

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.numberniceic.ui.screens.ChatScreen

class ChatActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = intent.getStringExtra("session_id")
        setContent {
            ChatScreen(
                onBack = { finish() },
                initialSessionId = sessionId
            )
        }
    }
}
