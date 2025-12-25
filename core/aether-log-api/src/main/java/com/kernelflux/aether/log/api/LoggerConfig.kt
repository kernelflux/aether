package com.kernelflux.aether.log.api

/**
 * 日志配置类
 * 支持为不同的模块/组件配置独立的日志行为
 */
data class LoggerConfig(
    /**
     * 日志级别
     */
    val level: LogLevel = LogLevel.DEBUG,
    
    /**
     * 是否启用日志
     */
    val enabled: Boolean = true,
    
    /**
     * Tag 前缀（用于模块/组件区分）
     * 例如："PaymentModule" -> 所有日志的 tag 会变成 "PaymentModule:xxx"
     */
    val tagPrefix: String? = null,
    
    /**
     * 是否输出到控制台
     */
    val consoleEnabled: Boolean = true,
    
    /**
     * 是否输出到文件
     */
    val fileEnabled: Boolean = true,
    
    /**
     * 文件输出配置（可选）
     */
    val fileConfig: FileConfig? = null,
    
    /**
     * 自定义 so 库加载器（可选）
     * 用于支持应用层使用自定义的 so 加载器，如 ReLinker
     * 
     * 注意：此配置仅在 XLogLogger 实现中生效，AndroidLogLogger 不使用此配置
     * 
     * 示例：
     * ```kotlin
     * val config = LoggerConfig(
     *     libraryLoader = object : LibraryLoader {
     *         override fun loadLibrary(libName: String) {
     *             ReLinker.loadLibrary(context, libName)
     *         }
     *     }
     * )
     * ```
     */
    val libraryLoader: LibraryLoader? = null
)

/**
 * 文件输出配置
 */
data class FileConfig(
    /**
     * 日志文件目录
     */
    val logDir: String,

    /**
     * 缓存目录
     */
    val cacheDir: String,

    /**
     * 文件名前缀
     */
    val namePrefix: String = "aether",

    /**
     * 最大文件大小（字节）
     */
    val maxFileSize: Long = 10 * 1024 * 1024, // 10MB

    /**
     * 最大保留时间（毫秒）
     */
    val maxAliveTime: Long = 7 * 24 * 60 * 60 * 1000L, // 7天

    /**
     * 缓存日志保留天数
     * 用于控制缓存目录中日志文件的保留时间
     * 超过此天数的缓存日志会被自动移动到日志目录或删除
     * 0 表示不启用缓存日志功能
     */
    val cacheDays: Int = 0,

    /**
     * 加密公钥（可选，用于日志加密）
     * 如果提供，日志文件会被加密（需要 128 个字符的十六进制字符串）
     * 如果为 null，日志文件不会被加密
     */
    val pubkey: String? = null,

    /**
     * 是否启用压缩（默认 true）
     * true：日志文件会被 zlib 压缩，节省空间
     * false：日志文件不压缩，但仍然是二进制格式（包含 Header + Body + Tailer）
     * 
     * 注意：
     * - 即使不压缩，日志文件也不是纯文本格式，无法直接用文本编辑器打开
     * - 日志文件格式：Header（73字节）+ Body（日志内容）+ Tailer（1字节）
     * - 这是 Mars xlog 的设计，用于支持日志修复、按小时查询等功能
     * - 开发时建议使用 Logcat 查看日志，或使用 `strings` 命令提取文本内容
     * - 生产环境建议使用 Mars 官方工具解析日志文件
     */
    val compressEnabled: Boolean = true,

    /**
     * 自定义日志头部信息（可选）
     * 用于在日志文件开头添加自定义信息，如设备信息、IP 地址等
     * 
     * 格式：Map<String, String>，键值对会自动格式化为 "Key: Value" 的形式
     * 例如：
     * ```kotlin
     * customHeaderInfo = mapOf(
     *     "Device" to "Pixel 5",
     *     "IP" to "192.168.1.100",
     *     "OS" to "Android 12"
     * )
     * ```
     * 
     * 这些信息会在日志初始化时写入日志文件头部，位于 Aether xlog 基础编译信息之后
     * 
     * 注意：Aether xlog 的基础编译信息（AETHER_URL, AETHER_PATH, AETHER_REVISION 等）会始终保留
     */
    val customHeaderInfo: Map<String, String>? = null
)
