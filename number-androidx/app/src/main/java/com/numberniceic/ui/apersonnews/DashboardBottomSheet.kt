package com.numberniceic.ui.apersonnews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.numberniceic.R
import android.content.Intent
import com.numberniceic.ui.ChatComposeActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.Log

class DashboardBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("DashboardBottomSheet", "onViewCreated savedInstanceState=${savedInstanceState != null} args=$arguments")

        if (savedInstanceState == null) {
            // Load PersonNewsF into the container
            val force = arguments?.getBoolean("force_refresh", false) ?: false
            Log.d("DashboardBottomSheet", "Attaching PersonNewsF force_refresh=$force")
            val fragment = PersonNewsF()
            if (force) {
                val args = Bundle()
                args.putBoolean("force_refresh", true)
                fragment.arguments = args
            }
            
            // Using childFragmentManager strictly because we are inside a Fragment (BottomSheetDialogFragment)
            val tx = childFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_dashboard_bs, fragment, "PersonNewsF")
            try {
                tx.commitNow()
                Log.d("DashboardBottomSheet", "PersonNewsF commitNow success")
            } catch (e: Exception) {
                Log.w("DashboardBottomSheet", "PersonNewsF commitNow failed, fallback commit", e)
                tx.commit()
            }
        }
        

    }
    
    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
            val displayMetrics = resources.displayMetrics
            val totalHeight = displayMetrics.heightPixels
            val targetHeight = (totalHeight * 0.95).toInt() // Expand to 95% to reach tab bar

            sheet.layoutParams.height = targetHeight
            behavior.isFitToContents = false
            behavior.expandedOffset = totalHeight - targetHeight
            behavior.peekHeight = targetHeight
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
    }
}
