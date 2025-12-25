package com.kernelflux.aether.ui.toast

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

/**
 * * Hook Context
 **/
class SafeToastContext(base: Context?, private val safeToastListener: ISafeToastListener?) :
    ContextWrapper(base) {
    private inner class CustomContextWrapper(base: Context?) : ContextWrapper(base) {
        override fun getSystemService(name: String): Any {
            return if ("window" == name) {
                WindowManagerWrapper(baseContext.getSystemService(name) as WindowManager)
            } else super.getSystemService(name)
        }
    }

    private inner class WindowManagerWrapper(private val windowManager: WindowManager) :
        WindowManager {
        @Deprecated("Deprecated in Java")
        override fun getDefaultDisplay(): Display {
            return windowManager.defaultDisplay
        }

        override fun removeViewImmediate(view: View) {
            windowManager.removeViewImmediate(view)
        }

        override fun addView(view: View, params: ViewGroup.LayoutParams) {
            try {
                windowManager.addView(view, params)
            } catch (e: WindowManager.BadTokenException) {
                Log.d("WindowManagerWrapper", "$e.message")
                safeToastListener?.logMsg("BadTokenException")
            } catch (e2: IllegalStateException) {
                Log.d("WindowManagerWrapper", "$e2.message")
                safeToastListener?.logMsg("IllegalStateException")
            } catch (th: Throwable) {
                Log.d("WindowManagerWrapper", "$th.message")
                safeToastListener?.logMsg(th.javaClass.simpleName)
            }
        }

        override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) {
            windowManager.updateViewLayout(view, params)
        }

        override fun removeView(view: View) {
            windowManager.removeView(view)
        }
    }

    override fun getApplicationContext(): Context {
        return CustomContextWrapper(baseContext.applicationContext)
    }

    companion object {
        @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
        fun hookViewContext(view: View?, context: Context?) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                try {
                    val declaredFiled = View::class.java.getDeclaredField("mContext")
                    declaredFiled.isAccessible = true
                    declaredFiled[view] = context
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
        }
    }
}