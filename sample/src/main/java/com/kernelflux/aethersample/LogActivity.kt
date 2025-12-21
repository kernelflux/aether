package com.kernelflux.aethersample

import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.kernelflux.aether.log.api.AppenderMode
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.fluxrouter.core.FluxRouter

/**
 * 日志服务示例 Activity
 * 展示 Aether Log API 的各种使用场景
 *
 * @author Aether Framework
 */
class LogActivity : BaseActivity() {

    private val logger: ILogger? = FluxRouter.getService(ILogger::class.java)

    private lateinit var logOutput: TextView

    override fun getContentResId(): Int = R.layout.activity_log

    override fun onInitView() {
        logOutput = findViewById(R.id.log_output)
        
        // 基础日志示例
        findViewById<Button>(R.id.btn_basic_log).setOnClickListener {
            testBasicLogging()
        }

        // 不同级别日志示例
        findViewById<Button>(R.id.btn_level_log).setOnClickListener {
            testLogLevels()
        }

        // 异常日志示例
        findViewById<Button>(R.id.btn_exception_log).setOnClickListener {
            testExceptionLogging()
        }

        // 多模块日志示例
        findViewById<Button>(R.id.btn_module_log).setOnClickListener {
            testModuleLogging()
        }

        // 动态配置示例
        findViewById<Button>(R.id.btn_dynamic_config).setOnClickListener {
            testDynamicConfig()
        }

        // 日志刷新示例
        findViewById<Button>(R.id.btn_flush_log).setOnClickListener {
            testFlushLogging()
        }

        // Appender 模式切换示例
        findViewById<Button>(R.id.btn_appender_mode).setOnClickListener {
            testAppenderMode()
        }

        // 获取日志级别示例
        findViewById<Button>(R.id.btn_get_level).setOnClickListener {
            testGetLogLevel()
        }

        // 性能测试示例
        findViewById<Button>(R.id.btn_performance).setOnClickListener {
            testPerformance()
        }

        // 清空输出
        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            logOutput.text = ""
        }

        // 初始日志
        appendLog("=== Aether Log Service Demo ===\n")
        appendLog("点击按钮测试各种日志功能\n\n")
    }

    /**
     * 基础日志示例
     */
    private fun testBasicLogging() {
        appendLog("\n--- 基础日志示例 ---\n")
        
        // 检查 logger 是否可用
        if (logger == null) {
            appendLog("❌ 错误：日志服务未找到，请检查是否已注册 XLogLogger\n")
            android.util.Log.e("LogActivity", "Logger service is null")
            return
        }
        
        // 检查日志服务是否已初始化
        val logLevel = logger.getLogLevel()
        appendLog("当前日志级别：${logLevel ?: "未初始化"}\n")
        
        // 确保日志级别设置为 DEBUG（这样 VERBOSE 会被过滤，但其他级别会输出）
        logger.setLogLevel(LogLevel.DEBUG)
        appendLog("已设置日志级别为 DEBUG\n")
        
        // 输出日志
        appendLog("开始输出日志...\n")
        logger.v("Basic", "Verbose 日志：最详细的日志信息（可能被过滤）")
        logger.d("Basic", "Debug 日志：调试信息")
        logger.i("Basic", "Info 日志：一般信息")
        logger.w("Basic", "Warn 日志：警告信息")
        logger.e("Basic", "Error 日志：错误信息")
        appendLog("已输出 5 条不同级别的日志，请查看 Logcat\n")
        appendLog("注意：VERBOSE 级别可能被过滤（当前级别为 DEBUG）\n")
    }

    /**
     * 不同级别日志示例
     */
    private fun testLogLevels() {
        appendLog("\n--- 日志级别测试 ---\n")
        
        // 设置为 VERBOSE 级别（显示所有日志）
        logger?.setLogLevel(LogLevel.VERBOSE)
        appendLog("设置日志级别为 VERBOSE\n")
        logger?.v("Level", "VERBOSE 级别日志")
        logger?.d("Level", "DEBUG 级别日志")
        logger?.i("Level", "INFO 级别日志")
        logger?.w("Level", "WARN 级别日志")
        logger?.e("Level", "ERROR 级别日志")

        // 设置为 INFO 级别（只显示 INFO、WARN、ERROR）
        logger?.setLogLevel(LogLevel.INFO)
        appendLog("\n设置日志级别为 INFO（VERBOSE 和 DEBUG 将被过滤）\n")
        logger?.v("Level", "VERBOSE 级别日志（被过滤）")
        logger?.d("Level", "DEBUG 级别日志（被过滤）")
        logger?.i("Level", "INFO 级别日志")
        logger?.w("Level", "WARN 级别日志")
        logger?.e("Level", "ERROR 级别日志")

        // 恢复为 DEBUG
        logger?.setLogLevel(LogLevel.DEBUG)
        appendLog("\n已恢复为 DEBUG 级别\n")
    }

    /**
     * 异常日志示例
     */
    private fun testExceptionLogging() {
        appendLog("\n--- 异常日志示例 ---\n")
        
        try {
            // 模拟一个异常
            throw IllegalArgumentException("这是一个测试异常")
        } catch (e: Exception) {
            logger?.e("Exception", "捕获到异常", e)
            appendLog("已输出异常日志，包含堆栈信息\n")
        }

        // 嵌套异常
        try {
            try {
                throw NullPointerException("内部异常")
            } catch (inner: Exception) {
                throw RuntimeException("外部异常", inner)
            }
        } catch (e: Exception) {
            logger?.e("Exception", "嵌套异常示例", e)
            appendLog("已输出嵌套异常日志\n")
        }
    }

    /**
     * 多模块日志示例
     */
    private fun testModuleLogging() {
        appendLog("\n--- 多模块日志示例 ---\n")
        
        // Payment 模块日志（通过 tag 区分模块）
        logger?.i("Payment", "处理支付请求")
        logger?.d("Payment", "支付金额：100.00 元")
        logger?.w("Payment", "支付可能耗时较长")
        
        // Network 模块日志（通过 tag 区分模块）
        logger?.d("Network", "发送 HTTP 请求")
        logger?.i("Network", "请求 URL: https://api.example.com/data")
        logger?.e("Network", "请求失败：网络超时")
        
        appendLog("已输出不同模块的日志，注意 tag 前缀的区别\n")
    }

    /**
     * 动态配置示例
     */
    private fun testDynamicConfig() {
        appendLog("\n--- 动态配置示例 ---\n")
        
        val currentLevel = logger?.getLogLevel()
        appendLog("当前日志级别：${currentLevel}\n")
        
        // 动态禁用日志
        logger?.setEnabled(false)
        appendLog("已禁用日志\n")
        logger?.d("Config", "这条日志不会输出（已禁用）")
        
        // 重新启用
        logger?.setEnabled(true)
        appendLog("已重新启用日志\n")
        logger?.d("Config", "这条日志会输出（已启用）")
        
        // 动态修改日志级别
        logger?.setLogLevel(LogLevel.WARN)
        appendLog("已设置日志级别为 WARN\n")
        logger?.d("Config", "DEBUG 日志（被过滤）")
        logger?.w("Config", "WARN 日志（会输出）")
        
        // 恢复
        logger?.setLogLevel(LogLevel.DEBUG)
        appendLog("已恢复为 DEBUG 级别\n")
    }

    /**
     * 日志刷新示例
     */
    private fun testFlushLogging() {
        appendLog("\n--- 日志刷新示例 ---\n")
        
        logger?.d("Flush", "写入大量日志...")
        for (i in 1..100) {
            logger?.d("Flush", "日志条目 $i")
        }
        
        appendLog("已写入 100 条日志\n")
        appendLog("执行异步刷新...\n")
        logger?.flush(isSync = false)
        
        appendLog("执行同步刷新（确保立即写入磁盘）...\n")
        logger?.flush(isSync = true)
        appendLog("刷新完成\n")
    }

    /**
     * Appender 模式切换示例
     */
    private fun testAppenderMode() {
        appendLog("\n--- Appender 模式切换示例 ---\n")
        
        appendLog("切换到同步模式（确保日志不丢失）...\n")
        logger?.setAppenderMode(AppenderMode.SYNC)
        logger?.e("Mode", "关键错误日志（同步模式，立即写入）")
        
        appendLog("切换到异步模式（性能更好）...\n")
        logger?.setAppenderMode(AppenderMode.ASYNC)
        logger?.d("Mode", "普通日志（异步模式，后台写入）")
        
        appendLog("模式切换完成\n")
    }

    /**
     * 获取日志级别示例
     */
    private fun testGetLogLevel() {
        appendLog("\n--- 获取日志级别示例 ---\n")
        
        val level = logger?.getLogLevel()
        appendLog("当前日志级别：$level\n")
        
        logger?.setLogLevel(LogLevel.INFO)
        val newLevel = logger?.getLogLevel()
        appendLog("设置后日志级别：$newLevel\n")
        
        logger?.setLogLevel(LogLevel.DEBUG)
        appendLog("已恢复为 DEBUG\n")
    }

    /**
     * 性能测试示例
     */
    private fun testPerformance() {
        appendLog("\n--- 性能测试示例 ---\n")
        
        val count = 1000
        appendLog("开始写入 $count 条日志...\n")
        
        val startTime = System.currentTimeMillis()
        for (i in 1..count) {
            logger?.d("Performance", "性能测试日志 $i")
        }
        val endTime = System.currentTimeMillis()
        
        val duration = endTime - startTime
        appendLog("完成！写入 $count 条日志耗时：${duration}ms\n")
        appendLog("平均每条日志：${duration.toFloat() / count}ms\n")
        
        // 刷新确保所有日志写入
        logger?.flush(isSync = true)
        appendLog("已刷新日志缓冲区\n")
    }

    private fun appendLog(message: String) {
        logOutput.append(message)
        val scrollView = findViewById<ScrollView>(R.id.scroll_view)
        scrollView.post {
            scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }
}

