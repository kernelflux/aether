package com.kernelflux.aether.network.api

/**
 * 网络配置
 */
data class NetworkConfig(
    /**
     * 基础 URL
     */
    val baseUrl: String,
    
    /**
     * 连接超时时间（毫秒）
     */
    val connectTimeout: Long = 30_000,
    
    /**
     * 读取超时时间（毫秒）
     */
    val readTimeout: Long = 30_000,
    
    /**
     * 写入超时时间（毫秒）
     */
    val writeTimeout: Long = 30_000,
    
    /**
     * 默认请求头
     */
    val defaultHeaders: Map<String, String> = emptyMap(),
    
    /**
     * 拦截器列表
     */
    val interceptors: List<Interceptor> = emptyList(),
    
    /**
     * 网络层拦截器列表
     */
    val networkInterceptors: List<Interceptor> = emptyList(),
    
    /**
     * 缓存配置
     */
    val cache: Cache? = null,
    
    /**
     * 默认缓存策略
     */
    val defaultCacheStrategy: CacheStrategy = CacheStrategy.NO_CACHE,
    
    /**
     * 默认重试策略
     */
    val defaultRetryStrategy: RetryStrategy? = null,
    
    /**
     * 证书校验器
     */
    val certificateValidator: CertificateValidator? = null,
    
    /**
     * DNS 解析器
     */
    val dnsResolver: DnsResolver = SystemDnsResolver,
    
    /**
     * 默认数据转换器
     */
    val defaultDataConverter: DataConverter? = null,
    
    /**
     * 弱网处理器
     */
    val weakNetworkHandler: WeakNetworkHandler? = null,
    
    /**
     * 网络质量检测器
     */
    val networkQualityDetector: NetworkQualityDetector? = null,
    
    /**
     * 设备性能检测器
     */
    val devicePerformanceDetector: DevicePerformanceDetector? = null,
    
    /**
     * 请求优化器
     */
    val requestOptimizer: RequestOptimizer? = null,
    
    /**
     * ROM 兼容性处理器
     */
    val romCompatibilityHandler: RomCompatibilityHandler? = null,
    
    /**
     * 网络策略
     */
    val networkStrategy: NetworkStrategy = DefaultNetworkStrategy.create(),
    
    /**
     * 网络状态管理器
     */
    val networkStateManager: NetworkStateManager? = null,
    
    /**
     * 网络状态监听器
     */
    val networkStateListener: NetworkStateListener? = null,
    
    /**
     * 是否启用日志
     */
    val enableLogging: Boolean = false,
    
    /**
     * 是否允许重定向
     */
    val followRedirects: Boolean = true,
    
    /**
     * 最大重定向次数
     */
    val maxRedirects: Int = 5,
    
    /**
     * 是否启用 Cookie
     */
    val enableCookies: Boolean = true
) {
    /**
     * 配置构建器
     */
    class Builder {
        private var baseUrl: String = ""
        private var connectTimeout: Long = 30_000
        private var readTimeout: Long = 30_000
        private var writeTimeout: Long = 30_000
        private var defaultHeaders: MutableMap<String, String> = mutableMapOf()
        private var interceptors: MutableList<Interceptor> = mutableListOf()
        private var networkInterceptors: MutableList<Interceptor> = mutableListOf()
        private var cache: Cache? = null
        private var defaultCacheStrategy: CacheStrategy = CacheStrategy.NO_CACHE
        private var defaultRetryStrategy: RetryStrategy? = null
        private var certificateValidator: CertificateValidator? = null
        private var dnsResolver: DnsResolver = SystemDnsResolver
        private var defaultDataConverter: DataConverter? = null
        private var weakNetworkHandler: WeakNetworkHandler? = null
        private var networkQualityDetector: NetworkQualityDetector? = null
        private var devicePerformanceDetector: DevicePerformanceDetector? = null
        private var requestOptimizer: RequestOptimizer? = null
        private var romCompatibilityHandler: RomCompatibilityHandler? = null
        private var networkStrategy: NetworkStrategy = DefaultNetworkStrategy.create()
        private var networkStateManager: NetworkStateManager? = null
        private var networkStateListener: NetworkStateListener? = null
        private var enableLogging: Boolean = false
        private var followRedirects: Boolean = true
        private var maxRedirects: Int = 5
        private var enableCookies: Boolean = true
        
        fun baseUrl(url: String) = apply { this.baseUrl = url }
        fun connectTimeout(timeout: Long) = apply { this.connectTimeout = timeout }
        fun readTimeout(timeout: Long) = apply { this.readTimeout = timeout }
        fun writeTimeout(timeout: Long) = apply { this.writeTimeout = timeout }
        fun defaultHeader(name: String, value: String) = apply { this.defaultHeaders[name] = value }
        fun defaultHeaders(headers: Map<String, String>) = apply { this.defaultHeaders.putAll(headers) }
        fun addInterceptor(interceptor: Interceptor) = apply { this.interceptors.add(interceptor) }
        fun addNetworkInterceptor(interceptor: Interceptor) = apply { this.networkInterceptors.add(interceptor) }
        fun cache(cache: Cache) = apply { this.cache = cache }
        fun defaultCacheStrategy(strategy: CacheStrategy) = apply { this.defaultCacheStrategy = strategy }
        fun defaultRetryStrategy(strategy: RetryStrategy) = apply { this.defaultRetryStrategy = strategy }
        fun certificateValidator(validator: CertificateValidator) = apply { this.certificateValidator = validator }
        fun dnsResolver(resolver: DnsResolver) = apply { this.dnsResolver = resolver }
        fun defaultDataConverter(converter: DataConverter) = apply { this.defaultDataConverter = converter }
        fun weakNetworkHandler(handler: WeakNetworkHandler) = apply { this.weakNetworkHandler = handler }
        fun networkQualityDetector(detector: NetworkQualityDetector) = apply { this.networkQualityDetector = detector }
        fun devicePerformanceDetector(detector: DevicePerformanceDetector) = apply { this.devicePerformanceDetector = detector }
        fun requestOptimizer(optimizer: RequestOptimizer) = apply { this.requestOptimizer = optimizer }
        fun romCompatibilityHandler(handler: RomCompatibilityHandler) = apply { this.romCompatibilityHandler = handler }
        fun networkStrategy(strategy: NetworkStrategy) = apply { this.networkStrategy = strategy }
        fun networkStateManager(manager: NetworkStateManager) = apply { this.networkStateManager = manager }
        fun networkStateListener(listener: NetworkStateListener) = apply { this.networkStateListener = listener }
        fun enableLogging(enable: Boolean) = apply { this.enableLogging = enable }
        fun followRedirects(follow: Boolean) = apply { this.followRedirects = follow }
        fun maxRedirects(max: Int) = apply { this.maxRedirects = max }
        fun enableCookies(enable: Boolean) = apply { this.enableCookies = enable }
        
        fun build() = NetworkConfig(
            baseUrl = baseUrl,
            connectTimeout = connectTimeout,
            readTimeout = readTimeout,
            writeTimeout = writeTimeout,
            defaultHeaders = defaultHeaders,
            interceptors = interceptors,
            networkInterceptors = networkInterceptors,
            cache = cache,
            defaultCacheStrategy = defaultCacheStrategy,
            defaultRetryStrategy = defaultRetryStrategy,
            certificateValidator = certificateValidator,
            dnsResolver = dnsResolver,
            defaultDataConverter = defaultDataConverter,
            weakNetworkHandler = weakNetworkHandler,
            networkQualityDetector = networkQualityDetector,
            devicePerformanceDetector = devicePerformanceDetector,
            requestOptimizer = requestOptimizer,
            romCompatibilityHandler = romCompatibilityHandler,
            networkStrategy = networkStrategy,
            networkStateManager = networkStateManager,
            networkStateListener = networkStateListener,
            enableLogging = enableLogging,
            followRedirects = followRedirects,
            maxRedirects = maxRedirects,
            enableCookies = enableCookies
        )
    }
    
    companion object {
        fun builder() = Builder()
    }
}
