package com.kernelflux.aether.ui.toast

import android.os.Build
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

/**
 * * fix Toast error on OS7.1.x( android.view.WindowManager$BadTokenException)
 **/
object SafeToast {
    private val TAG = SafeToast::class.java.simpleName

    @JvmStatic
    fun hookToast(view: View, toast: Toast?, listener: ISafeToastListener?) {
        var isHook: Boolean
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
            val deviceBrand: String = Build.BRAND
            if (TextUtils.isEmpty(deviceBrand) || !deviceBrand.lowercase(Locale.ROOT)
                    .contains("nubia")
            ) {
                SafeToastContext.Companion.hookViewContext(view,
                    SafeToastContext(view.context, listener)
                )
                return
            }
            try {
                val mTNObj = getObject(toast, "mTN")
                if (mTNObj != null) {
                    val mTaskObj = getObject(mTNObj, "mShow")
                    isHook = if (mTaskObj == null || mTaskObj !is Runnable) {
                        false
                    } else {
                        hookFiled(mTaskObj, "mShow", RunnableProxy(mTaskObj))
                    }
                    if (!isHook) {
                        val mHandlerObj = getObject(mTaskObj, "mHandler")
                        if (mHandlerObj != null && mHandlerObj is Handler) {
                            isHook = hookFiled(
                                mHandlerObj, "mCallback", HandlerProxy(
                                    mHandlerObj
                                )
                            )
                        }
                    }
                    if (!isHook) {
                        Log.d(TAG, "tryToHook error.")
                    }
                }
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
    }

    private fun hookFiled(
        obj: Any,
        fieldName: String,
        fieldObjProxy: Any
    ): Boolean {
        val field = getField(obj, fieldName)
        if (field != null) {
            try {
                if (Modifier.isFinal(field.modifiers)) {
                    val declaredField = Field::class.java.getDeclaredField("accessFlags")
                    declaredField.isAccessible = true
                    declaredField.setInt(field, field.modifiers and -17)
                }
                if (!field.isAccessible) {
                    field.isAccessible = true
                }
                field[obj] = fieldObjProxy
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    private fun getObject(
        obj: Any?,
        fieldName: String
    ): Any? {
        return getFiledObject(obj, getField(obj, fieldName))
    }

    private fun getFiledObject(obj: Any?, field: Field?): Any? {
        if (field != null) {
            try {
                if (!field.isAccessible) {
                    field.isAccessible = true
                }
                return field[obj]
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun getField(obj: Any?, fieldName: String): Field? {
        var cls: Class<*>? = obj?.javaClass
        while (cls != Any::class.java) {
            if (cls == null) {
                return null
            }
            cls = try {
                return cls.getDeclaredField(fieldName)
            } catch (e: NoSuchFieldException) {
                cls.superclass
            }
        }
        return null
    }

    private class HandlerProxy(private val mHandler: Handler) : Handler() {
        override fun handleMessage(msg: Message) {
            try {
                mHandler.handleMessage(msg)
            } catch (th: Throwable) {
                //fix bad token bug
                th.printStackTrace()
            }
        }
    }

    private class RunnableProxy(private val runnable: Runnable) : Runnable {
        override fun run() {
            try {
                runnable.run()
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
    }
}