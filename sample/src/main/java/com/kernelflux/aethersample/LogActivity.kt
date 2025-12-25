package com.kernelflux.aethersample

import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.kernelflux.aether.log.api.AppenderMode
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LogFileInfo
import com.kernelflux.aether.log.api.LogFileInfoCallback
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.fluxrouter.core.FluxRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 日志服务示例 Activity
 * 展示 Aether Log API 的各种使用场景
 */
class LogActivity : BaseActivity() {

    private val logger: ILogger? = FluxRouter.getService(ILogger::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

        // 多模块并发测试
        findViewById<Button>(R.id.btn_module_concurrent).setOnClickListener {
            testModuleConcurrent()
        }

        // 刷新单个模块
        findViewById<Button>(R.id.btn_flush_module).setOnClickListener {
            testFlushModule()
        }

        // 批量刷新模块
        findViewById<Button>(R.id.btn_flush_modules).setOnClickListener {
            testFlushModules()
        }

        // 获取日志文件信息
        findViewById<Button>(R.id.btn_get_file_infos).setOnClickListener {
            testGetLogFileInfos()
        }

        // 异步获取日志文件信息（协程）
        findViewById<Button>(R.id.btn_get_file_infos_async).setOnClickListener {
            testGetLogFileInfosAsync()
        }

        // 异步获取日志文件信息（回调）
        findViewById<Button>(R.id.btn_get_file_infos_callback).setOnClickListener {
            testGetLogFileInfosCallback()
        }

        // 获取所有模块
        findViewById<Button>(R.id.btn_get_all_modules).setOnClickListener {
            testGetAllModules()
        }

        // 清除文件缓存
        findViewById<Button>(R.id.btn_clear_cache).setOnClickListener {
            testClearFileCache()
        }

        // 清空输出
        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            logOutput.text = ""
        }

        // 初始日志
        appendLog("=== Aether Log Service Demo ===\n")
        appendLog("点击按钮测试各种日志功能\n\n")
        
        // 确保 TextView 有最小高度，避免 ScrollView 高度为 0
        logOutput.minHeight = 100
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
     * 展示如何使用 withModule 为不同业务模块指定日志文件名
     * 每个模块的日志会写入独立的日志文件，便于按模块排查问题
     */
    private fun testModuleLogging() {
        appendLog("\n--- 多模块日志示例 ---\n")
        appendLog("使用 withModule 为不同模块指定日志文件名\n")
        appendLog("每个模块的日志会写入独立的日志文件\n\n")
        
        // 方式1：链式调用，为网络模块指定模块名
        appendLog("【网络模块日志 - network_YYYYMMDD.xlog】\n")
        logger?.withModule("network")?.d("Network", "发送 HTTP GET 请求")
        logger?.withModule("network")?.i("Network", "请求 URL: https://api.example.com/users")
        logger?.withModule("network")?.d("Network", "请求头: Content-Type=application/json")
        logger?.withModule("network")?.i("Network", "响应状态码: 200 OK")
        logger?.withModule("network")?.e("Network", "请求失败：网络超时", 
            java.net.SocketTimeoutException("Connection timeout"))
        appendLog("✅ 网络模块日志已写入 network_*.xlog 文件\n\n")
        
        // 方式2：链式调用，为支付模块指定模块名
        appendLog("【支付模块日志 - payment_YYYYMMDD.xlog】\n")
        logger?.withModule("payment")?.i("Payment", "处理支付请求")
        logger?.withModule("payment")?.d("Payment", "支付金额：¥99.99")
        logger?.withModule("payment")?.d("Payment", "支付方式：支付宝")
        logger?.withModule("payment")?.w("Payment", "支付可能耗时较长，请等待")
        logger?.withModule("payment")?.i("Payment", "支付成功，交易ID: T123456789")
        logger?.withModule("payment")?.e("Payment", "支付失败：余额不足", 
            IllegalStateException("Insufficient balance"))
        appendLog("✅ 支付模块日志已写入 payment_*.xlog 文件\n\n")
        
        // 方式3：链式调用，为账户模块指定模块名（包含异常）
        appendLog("【账户模块日志 - account_YYYYMMDD.xlog】\n")
        logger?.withModule("account")?.d("Account", "用户登录请求")
        logger?.withModule("account")?.d("Account", "用户名: testuser")
        logger?.withModule("account")?.i("Account", "用户信息：userId=12345, role=admin")
        logger?.withModule("account")?.i("Account", "登录成功，生成 token")
        logger?.withModule("account")?.e("Account", "登录失败：密码错误", 
            IllegalArgumentException("Invalid password"))
        logger?.withModule("account")?.w("Account", "账户状态异常，需要验证")
        appendLog("✅ 账户模块日志已写入 account_*.xlog 文件\n\n")
        
        // 方式4：普通日志（不使用 withModule，使用默认模块名）
        appendLog("【默认模块日志 - aether_YYYYMMDD.xlog】\n")
        logger?.d("Default", "普通日志，使用默认模块名")
        logger?.i("Default", "默认模块日志会写入默认日志文件")
        logger?.w("Default", "这是默认模块的警告日志")
        appendLog("✅ 默认模块日志已写入 aether_*.xlog 文件\n\n")
        
        // 方式5：演示多线程场景（每个线程使用不同模块）
        appendLog("【多线程多模块日志测试】\n")
        Thread {
            logger?.withModule("network")?.d("NetworkThread", "网络线程日志 1")
            logger?.withModule("network")?.d("NetworkThread", "网络线程日志 2")
        }.start()
        
        Thread {
            logger?.withModule("payment")?.d("PaymentThread", "支付线程日志 1")
            logger?.withModule("payment")?.d("PaymentThread", "支付线程日志 2")
        }.start()
        
        Thread {
            logger?.withModule("account")?.d("AccountThread", "账户线程日志 1")
            logger?.withModule("account")?.d("AccountThread", "账户线程日志 2")
        }.start()
        
        appendLog("✅ 多线程测试完成，每个线程的日志会写入对应模块的文件\n\n")
        
        appendLog("📝 总结：\n")
        appendLog("  • 不同模块的日志会写入不同的日志文件\n")
        appendLog("  • 文件命名格式：{模块名}_YYYYMMDD.xlog\n")
        appendLog("  • 使用 ThreadLocal 存储模块名，线程安全\n")
        appendLog("  • 便于业务侧按模块排查问题\n")
        appendLog("  • 支持链式调用，使用方便\n\n")
        
        appendLog("💡 提示：\n")
        appendLog("  日志文件位置：/data/data/{包名}/files/xlog/\n")
        appendLog("  可以使用 adb pull 命令导出日志文件查看\n")
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

    /**
     * 多模块并发测试示例
     * 展示多线程环境下，不同模块的日志如何正确写入各自的文件
     */
    private fun testModuleConcurrent() {
        appendLog("\n--- 多模块并发测试 ---\n")
        appendLog("启动多个线程，每个线程使用不同的模块名\n")
        appendLog("验证 ThreadLocal 机制确保线程安全\n\n")
        
        val threadCount = 5
        val logsPerThread = 20  // 增加日志量，确保能触发刷新
        val modules = listOf("network", "payment", "account", "order", "user")
        
        appendLog("启动 $threadCount 个线程，每个线程写入 $logsPerThread 条日志\n")
        
        val threads = mutableListOf<Thread>()
        val startTime = System.currentTimeMillis()
        
        for (i in 0 until threadCount) {
            val moduleName = modules[i % modules.size]
            val thread = Thread {
                val moduleLogger = logger?.withModule(moduleName)
                val threadName = Thread.currentThread().name
                for (j in 1..logsPerThread) {
                    // 写入不同级别的日志，确保有足够的数据量
                    when (j % 4) {
                        0 -> moduleLogger?.d("Concurrent", "线程 $threadName - 模块 $moduleName - DEBUG 日志 $j")
                        1 -> moduleLogger?.i("Concurrent", "线程 $threadName - 模块 $moduleName - INFO 日志 $j")
                        2 -> moduleLogger?.w("Concurrent", "线程 $threadName - 模块 $moduleName - WARN 日志 $j")
                        3 -> moduleLogger?.e("Concurrent", "线程 $threadName - 模块 $moduleName - ERROR 日志 $j")
                    }
                    // 不 sleep，快速写入，确保缓冲区有足够数据
                }
            }
            thread.name = "Thread-$i-$moduleName"
            threads.add(thread)
            thread.start()
        }
        
        // 等待所有线程完成
        threads.forEach { it.join() }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        appendLog("✅ 所有线程完成，总耗时：${duration}ms\n")
        appendLog("✅ 每个线程写入 $logsPerThread 条日志，总共 ${threadCount * logsPerThread} 条\n")
        appendLog("✅ 每个模块的日志应该写入对应的文件：\n")
        modules.forEach { module ->
            appendLog("   • ${module}_*.xlog\n")
        }
        appendLog("\n💡 提示：使用 adb pull 导出日志文件验证\n")
        
        // 刷新所有模块的日志缓冲区，确保日志已落盘
        appendLog("正在刷新所有模块的日志缓冲区...\n")
        logger?.flush(isSync = true)
        
        appendLog("✅ 已刷新所有模块的日志缓冲区\n")
        appendLog("\n📝 说明：\n")
        appendLog("   • mmap3 文件：内存映射文件，用于日志缓冲区\n")
        appendLog("   • xlog 文件：实际的日志文件\n")
        appendLog("   • 自动刷新：缓冲区达到 1/3 大小（约 50KB）或 FATAL 级别时触发\n")
        appendLog("   • 手动刷新：调用 flush() 强制刷新（测试场景使用）\n")
        appendLog("   • 性能优化：避免频繁刷新，提高日志写入性能\n")
        appendLog("\n💡 如果某些模块只有 mmap3 而没有 xlog：\n")
        appendLog("   • 说明日志还在缓冲区中，等待自动刷新（最多 15 分钟）\n")
        appendLog("   • 或者增加日志量（达到 1/3 缓冲区大小）触发自动刷新\n")
        appendLog("   • 或者调用 flush(isSync=true) 强制同步刷新\n")
    }

    /**
     * 刷新单个模块测试
     */
    private fun testFlushModule() {
        try {
            appendLog("\n--- 刷新单个模块测试 ---\n")
            
            if (logger == null) {
                appendLog("❌ 错误：日志服务未找到\n")
                return
            }
            
            // 先写入一些日志
            appendLog("为 account 模块写入日志...\n")
            logger.withModule("account")?.d("Account", "刷新测试日志 1")
            logger.withModule("account")?.i("Account", "刷新测试日志 2")
            logger.withModule("account")?.w("Account", "刷新测试日志 3")
            
            appendLog("执行异步刷新...\n")
            val asyncResult = logger?.flushModule("account", isSync = false)
            appendLog("异步刷新结果：${if (asyncResult == true) "成功" else "失败"}\n")
            
            appendLog("执行同步刷新（确保立即写入）...\n")
            val syncResult = logger?.flushModule("account", isSync = true)
            appendLog("同步刷新结果：${if (syncResult == true) "成功" else "失败"}\n")
            
            appendLog("✅ 刷新完成\n")
        } catch (e: Exception) {
            appendLog("❌ 异常：${e.message}\n")
            android.util.Log.e("LogActivity", "testFlushModule error", e)
        }
    }

    /**
     * 批量刷新多个模块测试
     */
    private fun testFlushModules() {
        try {
            appendLog("\n--- 批量刷新多个模块测试 ---\n")
            
            if (logger == null) {
                appendLog("❌ 错误：日志服务未找到\n")
                return
            }
            
            // 为多个模块写入日志
            val modules = listOf("network", "payment", "account")
            appendLog("为以下模块写入日志：${modules.joinToString(", ")}\n")
            
            modules.forEach { module ->
                logger?.withModule(module)?.d(module, "批量刷新测试日志")
                logger?.withModule(module)?.i(module, "准备刷新")
            }
            
            appendLog("执行批量异步刷新...\n")
            val asyncResults = logger?.flushModules(modules, isSync = false)
            appendLog("异步刷新结果：${asyncResults?.joinToString(", ") ?: "无"}\n")
            
            appendLog("执行批量同步刷新（确保立即写入）...\n")
            val syncResults = logger?.flushModules(modules, isSync = true)
            appendLog("同步刷新结果：${syncResults?.joinToString(", ") ?: "无"}\n")
            
            appendLog("✅ 批量刷新完成\n")
        } catch (e: Exception) {
            appendLog("❌ 异常：${e.message}\n")
            android.util.Log.e("LogActivity", "testFlushModules error", e)
        }
    }

    /**
     * 获取日志文件信息测试
     */
    private fun testGetLogFileInfos() {
        try {
            appendLog("\n--- 获取日志文件信息测试 ---\n")
            
            if (logger == null) {
                appendLog("❌ 错误：日志服务未找到\n")
                return
            }
            
            // 先写入一些日志并刷新
            appendLog("为 account 模块写入日志并刷新...\n")
            logger?.withModule("account")?.d("Account", "文件信息测试日志")
            logger?.flushModule("account", isSync = true)
            
            // 方式1：获取所有日志文件
            appendLog("\n【方式1：获取所有日志文件】\n")
            val allFiles = logger?.getLogFileInfos("account")
            appendLog("找到 ${allFiles?.size ?: 0} 个日志文件：\n")
            allFiles?.take(5)?.forEach { fileInfo ->
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(java.util.Date(fileInfo.lastModified))
                appendLog("  • ${fileInfo.path}\n")
                appendLog("    大小：${fileInfo.size} 字节，修改时间：$date\n")
                appendLog("    模块：${fileInfo.moduleName}，类型：${fileInfo.fileType}\n")
            }
            if ((allFiles?.size ?: 0) > 5) {
                appendLog("  ... 还有 ${(allFiles?.size ?: 0) - 5} 个文件\n")
            }
            
            // 方式2：获取指定天数前的日志文件
            appendLog("\n【方式2：获取 1 天前的日志文件】\n")
            val files1DayAgo = logger?.getLogFileInfos("account", daysAgo = 1)
            appendLog("找到 ${files1DayAgo?.size ?: 0} 个日志文件（1 天前）\n")
            
            // 方式3：获取时间范围内的日志文件
            appendLog("\n【方式3：获取时间范围内的日志文件】\n")
            val now = System.currentTimeMillis()
            val oneDayAgo = now - 24 * 60 * 60 * 1000L
            val filesInRange = logger?.getLogFileInfos("account", startTime = oneDayAgo, endTime = now)
            appendLog("找到 ${filesInRange?.size ?: 0} 个日志文件（最近 24 小时）\n")
            
            appendLog("✅ 文件信息获取完成\n")
        } catch (e: Exception) {
            appendLog("❌ 异常：${e.message}\n")
            android.util.Log.e("LogActivity", "testGetLogFileInfos error", e)
        }
    }

    /**
     * 异步获取日志文件信息测试（协程版本）
     */
    private fun testGetLogFileInfosAsync() {
        try {
            appendLog("\n--- 异步获取日志文件信息测试（协程版本）---\n")
            
            if (logger == null) {
                appendLog("❌ 错误：日志服务未找到\n")
                return
            }
            
            // 先写入一些日志并刷新
            appendLog("为 account 模块写入日志并刷新...\n")
            logger?.withModule("account")?.d("Account", "异步测试日志")
            logger?.flushModule("account", isSync = true)
            
            appendLog("使用协程异步获取日志文件信息...\n")
            coroutineScope.launch(Dispatchers.Main) {
                try {
                    val now = System.currentTimeMillis()
                    val oneDayAgo = now - 24 * 60 * 60 * 1000L
                    val files = logger?.getLogFileInfosAsync("account", startTime = oneDayAgo, endTime = now)
                    
                    appendLog("✅ 协程获取完成，找到 ${files?.size ?: 0} 个日志文件\n")
                    files?.take(3)?.forEach { fileInfo ->
                        appendLog("  • ${fileInfo.path} (${fileInfo.size} 字节)\n")
                    }
                } catch (e: Exception) {
                    appendLog("❌ 获取失败：${e.message}\n")
                    android.util.Log.e("LogActivity", "testGetLogFileInfosAsync error", e)
                }
            }
            
            appendLog("协程已启动，等待结果...\n")
        } catch (e: Exception) {
            appendLog("❌ 异常：${e.message}\n")
            android.util.Log.e("LogActivity", "testGetLogFileInfosAsync error", e)
        }
    }

    /**
     * 异步获取日志文件信息测试（回调版本）
     */
    private fun testGetLogFileInfosCallback() {
        try {
            appendLog("\n--- 异步获取日志文件信息测试（回调版本）---\n")
            
            if (logger == null) {
                appendLog("❌ 错误：日志服务未找到\n")
                return
            }
            
            // 先写入一些日志并刷新
            appendLog("为 account 模块写入日志并刷新...\n")
            logger?.withModule("account")?.d("Account", "回调测试日志")
            logger?.flushModule("account", isSync = true)
            
            val now = System.currentTimeMillis()
            val oneDayAgo = now - 24 * 60 * 60 * 1000L
            
            // 方式1：使用 Executor
            appendLog("\n【方式1：使用 Executor】\n")
            val executor = Executors.newSingleThreadExecutor()
            logger?.getLogFileInfosAsync(
                moduleName = "account",
                startTime = oneDayAgo,
                endTime = now,
                callback = object : LogFileInfoCallback {
                    override fun onSuccess(fileInfos: List<LogFileInfo>) {
                        mainHandler.post {
                            appendLog("✅ Executor 回调成功，找到 ${fileInfos.size} 个日志文件\n")
                            fileInfos.take(3).forEach { fileInfo ->
                                appendLog("  • ${fileInfo.path} (${fileInfo.size} 字节)\n")
                            }
                        }
                    }
                    
                    override fun onError(error: Throwable) {
                        mainHandler.post {
                            appendLog("❌ Executor 回调失败：${error.message}\n")
                        }
                    }
                },
                executor = executor
            )
            
            // 方式2：不使用 Executor（使用默认协程作用域）
            appendLog("\n【方式2：不使用 Executor（默认协程作用域）】\n")
            logger?.getLogFileInfosAsync(
                moduleName = "account",
                startTime = oneDayAgo,
                endTime = now,
                callback = object : LogFileInfoCallback {
                    override fun onSuccess(fileInfos: List<LogFileInfo>) {
                        mainHandler.post {
                            appendLog("✅ 默认回调成功，找到 ${fileInfos.size} 个日志文件\n")
                            fileInfos.take(3).forEach { fileInfo ->
                                appendLog("  • ${fileInfo.path} (${fileInfo.size} 字节)\n")
                            }
                        }
                    }
                    
                    override fun onError(error: Throwable) {
                        mainHandler.post {
                            appendLog("❌ 默认回调失败：${error.message}\n")
                        }
                    }
                }
            )
            
            appendLog("回调已启动，等待结果...\n")
        } catch (e: Exception) {
            appendLog("❌ 异常：${e.message}\n")
            android.util.Log.e("LogActivity", "testGetLogFileInfosCallback error", e)
        }
    }

    /**
     * 获取所有模块测试
     */
    private fun testGetAllModules() {
        try {
            appendLog("\n--- 获取所有模块测试 ---\n")
            
            if (logger == null) {
                appendLog("❌ 错误：日志服务未找到\n")
                return
            }
            
            // 先创建一些模块的日志
            appendLog("创建多个模块的日志...\n")
            val testModules = listOf("network", "payment", "account", "order", "user")
            testModules.forEach { module ->
                logger?.withModule(module)?.d(module, "模块测试日志")
            }
            
            appendLog("获取所有已注册的模块...\n")
            val allModules = logger?.getAllModules()
            appendLog("找到 ${allModules?.size ?: 0} 个模块：\n")
            allModules?.forEach { module ->
                appendLog("  • $module\n")
            }
            
            appendLog("✅ 模块列表获取完成\n")
        } catch (e: Exception) {
            appendLog("❌ 异常：${e.message}\n")
            android.util.Log.e("LogActivity", "testGetAllModules error", e)
        }
    }

    /**
     * 清除文件缓存测试
     */
    private fun testClearFileCache() {
        try {
            appendLog("\n--- 清除文件缓存测试 ---\n")
            
            if (logger == null) {
                appendLog("❌ 错误：日志服务未找到\n")
                return
            }
            
            // 先获取一些文件信息（会建立缓存）
            appendLog("获取 account 模块的日志文件信息（建立缓存）...\n")
            val filesBefore = logger.getLogFileInfos("account")
            appendLog("缓存前找到 ${filesBefore?.size ?: 0} 个文件\n")
            
            // 清除单个模块的缓存
            appendLog("清除 account 模块的缓存...\n")
            logger.clearFileCache("account")
            appendLog("✅ account 模块缓存已清除\n")
            
            // 再次获取（会重新扫描）
            appendLog("重新获取 account 模块的日志文件信息（重新扫描）...\n")
            val filesAfter = logger?.getLogFileInfos("account")
            appendLog("缓存清除后找到 ${filesAfter?.size ?: 0} 个文件\n")
            
            // 清除所有模块的缓存
            appendLog("清除所有模块的缓存...\n")
            logger.clearAllFileCache()
            appendLog("✅ 所有模块缓存已清除\n")
            
            appendLog("💡 提示：清除缓存后，下次获取文件信息时会重新扫描文件系统\n")
        } catch (e: Exception) {
            appendLog("❌ 异常：${e.message}\n")
            android.util.Log.e("LogActivity", "testClearFileCache error", e)
        }
    }

    private fun appendLog(message: String) {
        // 确保在主线程执行 UI 操作
        if (Looper.myLooper() == Looper.getMainLooper()) {
            logOutput.append(message)
            val scrollView = findViewById<ScrollView>(R.id.scroll_view)
            scrollView.post {
                scrollView.fullScroll(android.view.View.FOCUS_DOWN)
            }
        } else {
            // 如果不在主线程，切换到主线程执行
            mainHandler.post {
                logOutput.append(message)
                val scrollView = findViewById<ScrollView>(R.id.scroll_view)
                scrollView.post {
                    scrollView.fullScroll(android.view.View.FOCUS_DOWN)
                }
            }
        }
    }
}

