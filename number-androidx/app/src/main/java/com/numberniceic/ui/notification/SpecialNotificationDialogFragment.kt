package com.numberniceic.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.numberniceic.R
import com.numberniceic.utils.dismissAllBottomSheets
import com.numberniceic.utils.showSingle

class SpecialNotificationDialogFragment : BottomSheetDialogFragment() {

    private var title: String? = null
    private var body: String? = null
    private var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString(ARG_TITLE)
            body = it.getString(ARG_BODY)
            url = it.getString(ARG_URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_special_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        view.findViewById<TextView>(R.id.txt_special_title).text = title
        view.findViewById<TextView>(R.id.txt_special_body).text = body
        val btnGoDashboard = view.findViewById<View>(R.id.btn_go_dashboard)
        
        // Enable dashboard button for all notifications
        btnGoDashboard.isVisible = true
        btnGoDashboard.setOnClickListener {
            try {
                // Use centralized utility to dismiss all bottom sheets
                parentFragmentManager.dismissAllBottomSheets()
                
                val act = activity
                if (act is com.numberniceic.ui.AppActivity) {
                    act.showDashboard(forceRefresh = true)
                } else {
                    val dashboardBS = com.numberniceic.ui.apersonnews.DashboardBottomSheet()
                    dashboardBS.showSingle(parentFragmentManager, "DashboardBottomSheet")
                }
                dismiss() // Close this dialog
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val btnClose = view.findViewById<TextView>(R.id.btn_close_special)
        
        if (!url.isNullOrEmpty()) {
            btnClose.text = "รับทราบ"
            btnClose.setOnClickListener {
                // Send refresh broadcast before closing
                requireContext().sendBroadcast(android.content.Intent("com.numberniceic.REFRESH_DASHBOARD"))
                dismiss()
            }
        } else {
             btnClose.setOnClickListener {
                // Send refresh broadcast
                requireContext().sendBroadcast(android.content.Intent("com.numberniceic.REFRESH_DASHBOARD"))
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // Mark as read when closed
        if (title != null && body != null) {
            val updated = com.numberniceic.data.local.NotificationStorage.markReadByContent(requireContext(), title!!, body!!)
            if (updated) {
                // Refresh Menu Badge & Notifications List
                requireContext().sendBroadcast(android.content.Intent("com.numberniceic.NEW_NOTIFICATION"))
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialogTheme
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_BODY = "body"
        private const val ARG_URL = "url"

        @JvmStatic
        fun newInstance(title: String, body: String, url: String? = null) =
            SpecialNotificationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_BODY, body)
                    putString(ARG_URL, url)
                }
            }
    }
}
