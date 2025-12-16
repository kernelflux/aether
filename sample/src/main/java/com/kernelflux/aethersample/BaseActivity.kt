package com.kernelflux.aethersample

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat

/**
 * Base Activity with EdgeToEdge support
 *
 * Automatically finds root view and applies window insets.
 * No need to set id="main" in layout files.
 *
 * @author Aether Framework
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable EdgeToEdge
        enableEdgeToEdge()

        // 设置状态栏字体为深色（黑色），适配白色背景
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        // Set content view (must be called by subclass)
        setContentView(getContentResId())

        // Apply window insets to root view
        setupWindowInsets()

        onInitView()
    }

    override fun onContentChanged() {
        super.onContentChanged()

    }

    /**
     * Setup window insets for EdgeToEdge
     * Automatically finds root view from content view
     */
    private fun setupWindowInsets() {
        val contentView = findViewById<ViewGroup>(android.R.id.content)
        val rootView = contentView?.getChildAt(0)
        rootView?.let { view ->
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                // 移除黑色背景设置，让布局文件中的背景色生效
                // v.setBackgroundResource(R.color.black)
                insets
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected abstract fun getContentResId(): Int
    protected abstract fun onInitView()
}
