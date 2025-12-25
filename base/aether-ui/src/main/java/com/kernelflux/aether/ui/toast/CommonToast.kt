package com.kernelflux.aether.ui.toast

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringRes
import com.kernelflux.aether.common.UtilsConfig
import com.kernelflux.aether.common.dp


object CommonToast {
    @JvmStatic
    private val TAG = CommonToast::class.java.simpleName

    @JvmStatic
    private val sHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    private var toast: Toast? = null

    @JvmStatic
    private val dp64: Int = 64f.dp

    @JvmStatic
    fun showToast(@StringRes textResId: Int) {
        val appContext = UtilsConfig.getAppContext() ?: return
        val content = try {
            appContext.resources.getString(textResId)
        } catch (th: Throwable) {
            th.printStackTrace()
            null
        }

        if (!content.isNullOrEmpty()) {
            showToast(content)
        }
    }

    @JvmStatic
    fun showToast(charSequence: CharSequence?) {
        if (!charSequence.isNullOrEmpty()) {
            sHandler.post {
                try {
                    if (toast == null) {
                        showNewToast(charSequence)
                    } else {
                        showOldToast(charSequence)
                    }
                } catch (th: Throwable) {
                    Log.d(TAG, Log.getStackTraceString(th))
                }
            }
        }
    }

    @JvmStatic
    private fun showOldToast(content: CharSequence) {
        try {
            toast?.cancel()
        } catch (e: NullPointerException) {
            //
        }
        toast = null
        showNewToast(content)
    }

    @JvmStatic
    private fun showNewToast(content: CharSequence) {
        val appCxt = UtilsConfig.getAppContext() ?: return
        val defaultToast = TextToast(appCxt)
        defaultToast.duration = Toast.LENGTH_SHORT
        defaultToast.setText(content)
        defaultToast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, dp64)
        toast = defaultToast
        try {
            toast?.show()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun dismissToast() {
        toast?.also {
            it.cancel()
            sHandler.removeCallbacksAndMessages(null)
        }
    }
}