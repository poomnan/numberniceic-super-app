package com.numberniceic.ui.apersonnews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.numberniceic.R
import android.widget.TextView

class MiraDiaryF : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme


    companion object {
        fun newInstance(content: String, title: String? = null, note: String? = null): MiraDiaryF {
            val args = Bundle()
            args.putString("content", content)
            args.putString("title", title)
            args.putString("note", note)
            val fragment = MiraDiaryF()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_mira_diary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString("title")
        val content = arguments?.getString("content") ?: arguments?.getString("mira")
        val note = arguments?.getString("note")

        view.findViewById<TextView>(R.id.txt_mira_title).text = title ?: "รายละเอียด"
        view.findViewById<TextView>(R.id.txt_mira_diary).text = content
        
        val txtNote = view.findViewById<TextView>(R.id.txt_mira_note)
        if (!note.isNullOrEmpty()) {
            txtNote.text = "คำเตือนพิเศษ: $note"
            txtNote.visibility = View.VISIBLE
        } else {
            txtNote.visibility = View.GONE
        }
        
        val btn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_accept_diary)
        btn.text = "รับทราบ"

        btn.setOnClickListener {
            dismiss()
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
            val targetHeight = (totalHeight * 0.85).toInt()

            sheet.layoutParams.height = targetHeight
            behavior.isFitToContents = false
            behavior.expandedOffset = totalHeight - targetHeight
            behavior.peekHeight = targetHeight
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
    }
}
