package com.sdei.imageupload.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.sdei.imageupload.R

class ProgressDialog(context: Context, var message: String) : Dialog(context, R.style.DialogTheme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_progress)

        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        val mProgressBar: ProgressBar = findViewById(R.id.circular_progress_bar)
        mProgressBar.isIndeterminate = true
        mProgressBar.indeterminateDrawable.setColorFilter(ContextCompat.getColor(context, android.R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)

        val messageTv: AppCompatTextView = findViewById(R.id.message_tv)
        messageTv.text = message
    }

}