package com.numberniceic.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.numberniceic.ui.screens.CartScreen

class CartComposeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialTabIndex = intent?.getIntExtra("initial_tab_index", 0) ?: 0
        setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            CartScreen(
                navController = navController,
                onBack = { finish() },
                initialTabIndex = initialTabIndex
            )
        }
    }
}
