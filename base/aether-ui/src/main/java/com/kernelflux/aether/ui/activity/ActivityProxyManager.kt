package com.kernelflux.aether.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * @author: QT
 * @date: 2025/12/24
 * Activity 代理管理器
 *
 * 管理多个 ActivityProxy，按顺序调用
 */
class ActivityProxyManager {
    private val proxies = mutableListOf<ActivityProxy>()

    @Synchronized
    fun register(proxy: ActivityProxy) {
        if (!proxies.contains(proxy)) {
            proxies.add(proxy)
        }
    }

    @Synchronized
    fun unregister(proxy: ActivityProxy) {
        proxies.remove(proxy)
    }

    fun onCreate(activity: BaseActivity, savedInstanceState: Bundle?) {
        proxies.forEach {
            try {
                it.onCreate(activity, savedInstanceState)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onStart(activity: BaseActivity) {
        proxies.forEach {
            try {
                it.onStart(activity)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onResume(activity: BaseActivity) {
        proxies.forEach {
            try {
                it.onResume(activity)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onPause(activity: BaseActivity) {
        proxies.forEach {
            try {
                it.onPause(activity)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onStop(activity: BaseActivity) {
        proxies.forEach {
            try {
                it.onStop(activity)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onDestroy(activity: BaseActivity) {
        proxies.forEach {
            try {
                it.onDestroy(activity)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onRestart(activity: BaseActivity) {
        proxies.forEach {
            try {
                it.onRestart(activity)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onActivityResult(activity: BaseActivity, requestCode: Int, resultCode: Int, data: Intent?) {
        proxies.forEach {
            try {
                it.onActivityResult(activity, requestCode, resultCode, data)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onConfigurationChanged(activity: BaseActivity, newConfig: Configuration) {
        proxies.forEach {
            try {
                it.onConfigurationChanged(activity, newConfig)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onContentChanged(activity: BaseActivity) {
        proxies.forEach {
            try {
                it.onContentChanged(activity)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onSaveInstanceState(activity: BaseActivity, outState: Bundle) {
        proxies.forEach {
            try {
                it.onSaveInstanceState(activity, outState)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onNewIntent(activity: BaseActivity, intent: Intent?) {
        proxies.forEach {
            try {
                it.onNewIntent(activity, intent)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onBackPressed(activity: BaseActivity): Boolean {
        return proxies.any {
            try {
                it.onBackPressed(activity)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun onMultiWindowModeChanged(activity: BaseActivity, isInMultiWindowMode: Boolean) {
        proxies.forEach {
            try {
                it.onMultiWindowModeChanged(activity, isInMultiWindowMode)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun onPictureInPictureModeChanged(
        activity: BaseActivity,
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        proxies.forEach {
            try {
                it.onPictureInPictureModeChanged(activity, isInPictureInPictureMode, newConfig)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun dispatchTouchEvent(activity: BaseActivity, ev: MotionEvent?): Boolean {
        return proxies.any {
            try {
                it.dispatchTouchEvent(activity, ev)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun onTouchEvent(activity: BaseActivity, event: MotionEvent?): Boolean {
        return proxies.any {
            try {
                it.onTouchEvent(activity, event)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun onKeyDown(activity: BaseActivity, keyCode: Int, event: KeyEvent?): Boolean {
        return proxies.any {
            try {
                it.onKeyDown(activity, keyCode, event)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun onKeyUp(activity: BaseActivity, keyCode: Int, event: KeyEvent?): Boolean {
        return proxies.any {
            try {
                it.onKeyUp(activity, keyCode, event)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun onWindowFocusChanged(activity: BaseActivity, hasFocus: Boolean) {
        proxies.forEach {
            try {
                it.onWindowFocusChanged(activity, hasFocus)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }

    fun attachBaseContext(activity: BaseActivity, newBase: Context?) {
        proxies.forEach {
            try {
                it.attachBaseContext(activity, newBase)
            } catch (e: Exception) {
                // 防止代理异常影响 Activity
            }
        }
    }
}