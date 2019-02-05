package com.example.parsaniahardik.kotlinselectimagegallery.dialog

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.example.parsaniahardik.kotlinselectimagegallery.R
import com.example.parsaniahardik.kotlinselectimagegallery.databinding.DialogPickSelectionBinding
import com.example.parsaniahardik.kotlinselectimagegallery.fragment.HomeFragment

class PickSelectionDialog : DialogFragment(), View.OnClickListener {

    private lateinit var binding: DialogPickSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onStart() {
        super.onStart()
        dialog!!.window!!.setWindowAnimations(R.style.dialog_slide_animation)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_pick_selection, container, false)
        binding.listener = this
        return binding.root
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.camera_ll -> {
                performAction(HomeFragment.ACTION_CAMERA)
            }
            R.id.gallery_ll -> {
                performAction(HomeFragment.ACTION_GALLERY)
            }
            R.id.remove_ll -> {
                performAction(HomeFragment.ACTION_REMOVE_PHOTO)
            }
            R.id.cancel_tv -> {
                targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, Intent())
                dialog!!.dismiss()
            }
        }
    }

    private fun performAction(action: Int) {
        val intent = Intent()
        intent.putExtra("action", action)
        targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
        dialog!!.dismiss()
    }

}