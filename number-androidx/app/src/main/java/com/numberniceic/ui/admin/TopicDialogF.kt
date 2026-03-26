package com.numberniceic.ui.admin


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment

import com.numberniceic.R
import com.numberniceic.databinding.FragmentTopicDialogBinding


class TopicDialogF : DialogFragment(), View.OnClickListener {


    private lateinit var binding: FragmentTopicDialogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_topic_dialog, container, false)
        binding.apply {
            lifecycleOwner = this@TopicDialogF
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnTopicList.setOnClickListener(this)
        binding.btnTopicNew.setOnClickListener(this)
    }



    override fun onClick(v: View?) {
       when(v){
           binding.btnTopicList -> {

           }
           binding.btnTopicNew -> {
               val intent = Intent(context, TopicAct::class.java)
                startActivity(intent)
                dismiss()
           }
       }
    }


}
