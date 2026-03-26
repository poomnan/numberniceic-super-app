package com.numberniceic.ui.tabian

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.numberniceic.ui.screens.LicensePlateScreen

class TabianHomeF : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("LIFECYCLE", "TAB TabianHome CREATE (Compose)")
        return ComposeView(requireContext()).apply {
            setContent {
                 // Using MaterialTheme to ensure proper styling for Compose components
                 androidx.compose.material3.MaterialTheme {
                     LicensePlateScreen(
                        navController = androidx.navigation.compose.rememberNavController(),
                        limitCount = 4
                     ) 
                 }
            }
        }
    }
}
