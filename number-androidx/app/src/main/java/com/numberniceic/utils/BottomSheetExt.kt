package com.numberniceic.utils

import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Extension function to dismiss all active BottomSheetDialogFragments in a FragmentManager.
 */
fun FragmentManager.dismissAllBottomSheets() {
    try {
        val fragments = this.fragments
        for (fragment in fragments) {
            if (fragment is BottomSheetDialogFragment && fragment.isAdded) {
                fragment.dismissAllowingStateLoss()
            }
            // Recurse into child fragment managers
            fragment.childFragmentManager.dismissAllBottomSheets()
        }
    } catch (e: Exception) {
        // Safe fail
    }
}

/**
 * Extension function to show a BottomSheetDialogFragment after dismissing all others.
 * This ensures no bottom sheets are overlapped.
 */
fun BottomSheetDialogFragment.showSingle(fragmentManager: FragmentManager, tag: String) {
    fragmentManager.dismissAllBottomSheets()
    this.show(fragmentManager, tag)
}
