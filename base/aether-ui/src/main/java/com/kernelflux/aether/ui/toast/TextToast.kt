package com.kernelflux.aether.ui.toast

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.kernelflux.aether.ui.R


class TextToast(context: Context?) : Toast(context), ISafeToastListener {
    private val textView: TextView
    override fun setText(s: CharSequence) {
        textView.text = s
    }

    override fun logMsg(message: String?) {
        //upload error log
    }

    init {
        @SuppressLint("InflateParams") val inflate: View =
            LayoutInflater.from(context).inflate(R.layout.text_toast, null)
        textView = inflate.findViewById<TextView>(R.id.tv_text_toast_content)
        view = inflate
        SafeToast.hookToast(inflate, this, this)
    }
}