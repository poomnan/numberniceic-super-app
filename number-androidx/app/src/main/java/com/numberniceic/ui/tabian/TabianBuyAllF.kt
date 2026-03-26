package com.numberniceic.ui.tabian

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.numberniceic.ui.screens.LicensePlateScreen
// import com.numberniceic.ui.theme.NumberNiceTheme 

class TabianBuyAllF : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("LIFECYCLE", "TabianBuyAllF (Compose)")
        return ComposeView(requireContext()).apply {
            setContent {
                 androidx.compose.material3.MaterialTheme {
                     // Reuse the same LicensePlateScreen which fetches all plates, but hide input and ViewAll button
                     LicensePlateScreen(
                        navController = androidx.navigation.compose.rememberNavController(),
                        showInputSection = false,
                        showViewAllButton = false
                     ) 
                 }
            }
        }
    }
}
