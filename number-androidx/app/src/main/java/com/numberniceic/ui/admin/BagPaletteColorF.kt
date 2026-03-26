package com.numberniceic.ui.admin

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.numberniceic.R
import com.numberniceic.databinding.FragmentBagPaletteColorBinding
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.lang.ClassCastException

class BagPaletteColorF : DialogFragment() {

    private var chipId: Int = 0
    private lateinit var binding: FragmentBagPaletteColorBinding
    private lateinit var onSelectListener: OnSelectColorListener

    companion object {
        fun newInstance(chipId: Int): BagPaletteColorF {
            val args = Bundle()
            args.putInt("chipId", chipId)
            val fragment = BagPaletteColorF()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CustomDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                               savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_bag_palette_color, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.chipId = arguments?.getInt("chipId") ?: 0

        setupColorPicker()

        binding.btnSelect.setOnClickListener {
            val color = binding.colorPickerView.color
            onSelectListener.selectListener(color, chipId)
            dismiss()
        }
    }

    private fun setupColorPicker() {
        binding.colorPickerView.attachBrightnessSlider(binding.brightnessSlide)
        
        binding.colorPickerView.setColorListener(ColorEnvelopeListener { envelope, _ ->
            binding.colorPreview.setBackgroundColor(envelope.color)
            binding.txtColorHex.text = "#${envelope.hexCode}"
        })
    }

    interface OnSelectColorListener {
        fun selectListener(defaultColor: Int, chipId: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onSelectListener = parentFragment as OnSelectColorListener
        } catch (c: ClassCastException) {
            Log.d("CLASSCAST", c.message ?: "Cast error")
        }
    }
}
