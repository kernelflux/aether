package com.kernelflux.aether.network.okhttp

import android.content.Context
import com.google.gson.Gson
import com.kernelflux.aether.network.api.CompressionAlgorithm
import com.kernelflux.aether.network.api.DeviceInfo
import com.kernelflux.aether.network.api.HttpMethod
import com.kernelflux.aether.network.api.CacheEntry
import com.kernelflux.aether.network.api.CacheStrategy
import com.kernelflux.aether.network.api.INetworkClient
import com.kernelflux.aether.network.api.NetworkCallback
import com.kernelflux.aether.network.api.NetworkConfig
import com.kernelflux.aether.network.api.NetworkException
import com.kernelflux.aether.network.api.NetworkState
import com.kernelflux.aether.network.api.NetworkStateListener
import com.kernelflux.aether.network.api.NetworkStateManager
import com.kernelflux.aether.network.api.NetworkUtils
import com.kernelflux.aether.network.api.NoNetworkResult
import com.kernelflux.aether.network.api.Response
import com.kernelflux.aether.network.api.SystemDnsResolver
import com.kernelflux.fluxrouter.annotation.FluxService
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * OkHttp 网络客户端实现
 *
 * 特性：
 * - 完善的缓存支持（使用应用缓存目录）
 * - 持久化 Cookie 管理
 * - 弱网优化
 * - 连接池管理
 * - HTTP/2 支持
 * - 请求优先级管理
 *
 * @author Aether Framework
 */
@FluxService(interfaceClass = INetworkClient::class)
class OkHttpNetworkClient : INetworkClient {
    
    companion object {
        // 使用静态变量存储初始化状态，确保所有实例共享
        private val okHttpClientRef = AtomicReference<OkHttpClient>()
        private val configRef = AtomicReference<NetworkConfig>()
        private val networkStateManagerRef = AtomicReference<NetworkStateManager>()
        private val cookieJarRef = AtomicReference<PersistentCookieJar>()
        private val cacheDirRef = AtomicReference<File>()
        private val scheduledExecutor: ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor()
    }
    
    private val gson = Gson()
    private val activeCalls = ConcurrentHashMap<Any, MutableList<Call>>()
    private val networkStateListeners = CopyOnWriteArrayList<NetworkStateListener>()
    private val networkStateRef = AtomicReference<NetworkState>(NetworkState.AVAILABLE)

    override fun init(context: Context, config: NetworkConfig) {
        android.util.Log.d("OkHttpNetworkClient", "init() called with context: ${context.javaClass.simpleName}, config: ${config.baseUrl}, instance: ${this.hashCode()}")
        
        try {
            // 如果用户没有提供默认实现，创建默认的 WeakNetworkHandler 和 RequestOptimizer
            val enhancedConfig = if (config.weakNetworkHandler == null || config.requestOptimizer == null) {
                val qualityDetector = config.networkQualityDetector 
                    ?: DefaultNetworkQualityDetector(context)
                val deviceDetector = config.devicePerformanceDetector 
                    ?: DefaultDevicePerformanceDetector(context)
                val requestOptimizer = config.requestOptimizer 
                    ?: DefaultRequestOptimizer()
                val weakNetworkHandler = config.weakNetworkHandler 
                    ?: DefaultWeakNetworkHandler(
                        qualityDetector = qualityDetector,
                        deviceDetector = deviceDetector,
                        requestOptimizer = requestOptimizer,
                        romHandler = config.romCompatibilityHandler
                    )
                
                // 创建新的配置，包含默认实现
                val builder = NetworkConfig.builder()
                    .baseUrl(config.baseUrl)
                    .connectTimeout(config.connectTimeout)
                    .readTimeout(config.readTimeout)
                    .writeTimeout(config.writeTimeout)
                    .followRedirects(config.followRedirects)
                    .enableLogging(config.enableLogging)
                    .enableCookies(config.enableCookies)
                    .cacheDir(config.cacheDir)
                    .defaultHeaders(config.defaultHeaders)
                    .defaultCacheStrategy(config.defaultCacheStrategy)
                    .weakNetworkHandler(weakNetworkHandler)
                    .requestOptimizer(requestOptimizer)
                    .networkQualityDetector(qualityDetector)
                    .devicePerformanceDetector(deviceDetector)
                
                // 可选参数
                config.defaultRetryStrategy?.let { builder.defaultRetryStrategy(it) }
                config.defaultDataConverter?.let { builder.defaultDataConverter(it) }
                config.networkStateManager?.let { builder.networkStateManager(it) }
                config.networkStrategy?.let { builder.networkStrategy(it) }
                config.certificateValidator?.let { builder.certificateValidator(it) }
                config.dnsResolver?.let { builder.dnsResolver(it) }
                config.cache?.let { builder.cache(it) }
                config.romCompatibilityHandler?.let { builder.romCompatibilityHandler(it) }
                
                // 拦截器
                config.interceptors.forEach { builder.addInterceptor(it) }
                config.networkInterceptors.forEach { builder.addNetworkInterceptor(it) }
                
                builder.build()
            } else {
                config
            }
            
            configRef.set(enhancedConfig)
            
            // 设置缓存目录（从 enhancedConfig 中获取，如果没有则使用应用缓存目录）
            val configuredCacheDir = enhancedConfig.cacheDir ?: run {
                val appContext = context.applicationContext ?: context
                File(appContext.cacheDir, "aether_network")
            }
            configuredCacheDir.mkdirs()
            cacheDirRef.set(configuredCacheDir)
            
            android.util.Log.d("OkHttpNetworkClient", "configRef.set() completed, cacheDir: ${configuredCacheDir.absolutePath}, instance: ${this.hashCode()}")

            // 初始化网络状态管理器
            val stateManager = try {
                enhancedConfig.networkStateManager ?: NetworkStateManagerFactory.create(context)
            } catch (e: Exception) {
                android.util.Log.e("OkHttpNetworkClient", "Failed to create NetworkStateManager", e)
                // 如果创建失败，使用一个简单的实现（不监听网络状态）
                null
            }
            if (stateManager != null) {
                networkStateManagerRef.set(stateManager)
                android.util.Log.d("OkHttpNetworkClient", "networkStateManager initialized")
            } else {
                android.util.Log.w("OkHttpNetworkClient", "NetworkStateManager is null, network state monitoring disabled")
            }

        // 初始化当前网络状态
        stateManager?.let { manager ->
            try {
                val currentState = manager.getCurrentState()
                networkStateRef.set(currentState)
            } catch (_: Exception) {
            }

            // 开始监听网络状态
            try {
                manager.startMonitoring()
                // 添加内部监听器，同步网络状态
                manager.addListener(object : NetworkStateListener {
                    override fun onNetworkStateChanged(oldState: NetworkState, newState: NetworkState) {
                        try {
                            networkStateRef.set(newState)

                            // 通知所有外部监听器（使用快照避免并发修改）
                            val snapshot = ArrayList(networkStateListeners)
                            snapshot.forEach { listener ->
                                try {
                                    listener.onNetworkStateChanged(oldState, newState)
                                } catch (_: Exception) {
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                })
            } catch (_: Exception) {
            }
        }

        // 根据弱网和设备信息优化连接池和超时
        val weakHandler = enhancedConfig.weakNetworkHandler
        val connectionPoolSize = weakHandler?.getSuggestedConnectionPoolSize() ?: 5
        val connectionPool = ConnectionPool(connectionPoolSize, 5, TimeUnit.MINUTES)

        val connectTimeout = if (weakHandler?.shouldReduceTimeout() == true) {
            weakHandler.getWeakNetworkTimeout()
        } else {
            config.connectTimeout
        }

        val readTimeout = if (weakHandler?.shouldReduceTimeout() == true) {
            weakHandler.getWeakNetworkTimeout()
        } else {
            config.readTimeout
        }

        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(config.writeTimeout, TimeUnit.MILLISECONDS)
            .followRedirects(config.followRedirects)
            .followSslRedirects(config.followRedirects)
            .connectionPool(connectionPool)
            // 启用 HTTP/2（如果弱网处理器建议）
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))

        // 添加应用层拦截器
        // 特点：在重定向和重试之前执行，即使从缓存返回也会执行
        config.interceptors.forEach { interceptor ->
            builder.addInterceptor(OkHttpInterceptorAdapter(interceptor, config))
        }

        // 添加网络层拦截器
        // 特点：在发送网络请求之前执行，只有真正的网络请求才会执行
        config.networkInterceptors.forEach { interceptor ->
            builder.addNetworkInterceptor(OkHttpInterceptorAdapter(interceptor, config))
        }

        // 缓存 - 使用应用缓存目录实现 OkHttp Cache
        val cacheDir = cacheDirRef.get()
        if (cacheDir != null) {
            // 使用应用缓存目录，默认 50MB
            val cacheSize = 50L * 1024 * 1024 // 50MB
            val okHttpCache = Cache(cacheDir, cacheSize)
            builder.cache(okHttpCache)
        }

        // DNS 解析器
        if (config.dnsResolver != SystemDnsResolver) {
            builder.dns(OkHttpDnsAdapter(config.dnsResolver))
        }

        // 证书校验（完整实现：X509TrustManager + HostnameVerifier）
        config.certificateValidator?.let { validator ->
            try {
                // 创建自定义 TrustManager（用于验证证书链）
                val trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                        // 客户端证书校验（通常不需要）
                    }

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                        // 服务器证书校验
                        if (chain == null || chain.isEmpty()) {
                            throw CertificateException("Certificate chain is empty")
                        }
                        
                        // 使用自定义验证器验证证书链（不验证 hostname，hostname 在 HostnameVerifier 中验证）
                        // 这里可以添加额外的证书链验证逻辑
                        val chainArray = Array(chain.size) { chain[it] }
                        val isValid = try {
                            validator.validate(chainArray, "")
                        } catch (e: Exception) {
                            false
                        }
                        
                        if (!isValid) {
                            throw CertificateException("Certificate validation failed")
                        }
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                }

                // 创建自定义 HostnameVerifier（用于验证 hostname 和证书）
                // OkHttp 使用 javax.net.ssl.HostnameVerifier
                val hostnameVerifier = javax.net.ssl.HostnameVerifier { hostname, session ->
                    try {
                        // 从 SSLSession 获取证书
                        val peerCertificates = try {
                            session.peerCertificates
                        } catch (e: Exception) {
                            null
                        }
                        
                        if (peerCertificates == null) {
                            false
                        } else {
                            val certificates = peerCertificates.mapNotNull { cert ->
                                cert as? X509Certificate
                            }.toTypedArray()
                            
                            if (certificates.isEmpty()) {
                                false
                            } else {
                                // 使用自定义验证器验证证书和主机名
                                validator.validate(certificates, hostname)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OkHttpNetworkClient", "Certificate validation failed", e)
                        false
                    }
                }

                // 配置 SSLContext（用于自定义 TrustManager）
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf<TrustManager>(trustManager), java.security.SecureRandom())
                
                builder.sslSocketFactory(sslContext.socketFactory, trustManager)
                builder.hostnameVerifier(hostnameVerifier)
            } catch (e: Exception) {
                android.util.Log.e("OkHttpNetworkClient", "Failed to configure certificate validator", e)
                // 如果配置失败，使用默认的证书验证（更安全）
            }
        }

        // Cookie - 使用持久化 Cookie 管理器
        if (config.enableCookies) {
            try {
                val cookieJar = cookieJarRef.get() ?: run {
                    // 如果没有提供 CookieJar，创建一个基于文件的持久化实现
                    val cookieDir =
                        cacheDir ?: File(System.getProperty("java.io.tmpdir"), "aether_cookies")
                    cookieDir.mkdirs()
                    val cookieFile = File(cookieDir, "cookies.dat")
                    val cookieStore = FileCookieStore(cookieFile)
                    val jar = PersistentCookieJar(cookieStore)

                    // 定期清理过期 Cookie（每小时）
                    scheduledExecutor.scheduleWithFixedDelay(
                        { jar.evictExpired() },
                        1, 1, TimeUnit.HOURS
                    )

                    cookieJarRef.set(jar)
                    jar
                }
                builder.cookieJar(cookieJar)
                android.util.Log.d("OkHttpNetworkClient", "CookieJar configured successfully")
            } catch (e: Exception) {
                android.util.Log.w("OkHttpNetworkClient", "Failed to configure CookieJar, continuing without cookies", e)
                // Cookie 配置失败不影响整体初始化，继续执行
            }
        }

        // 构建并设置 OkHttpClient（确保即使出错也能设置）
        android.util.Log.d("OkHttpNetworkClient", "Building OkHttpClient...")
        try {
            val okHttpClient = builder.build()
            okHttpClientRef.set(okHttpClient)
            android.util.Log.d("OkHttpNetworkClient", "okHttpClientRef.set() completed, client initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("OkHttpNetworkClient", "Failed to build OkHttpClient, trying fallback", e)
            // 如果构建失败，尝试使用最小配置构建
            try {
                val fallbackClient = OkHttpClient.Builder()
                    .connectTimeout(config.connectTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout(config.readTimeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(config.writeTimeout, TimeUnit.MILLISECONDS)
                    .build()
                okHttpClientRef.set(fallbackClient)
                android.util.Log.d("OkHttpNetworkClient", "Fallback OkHttpClient set successfully")
            } catch (e2: Exception) {
                android.util.Log.e("OkHttpNetworkClient", "Failed to build fallback OkHttpClient, using basic client", e2)
                // 如果还是失败，至少设置一个基本的客户端，确保不会为 null
                okHttpClientRef.set(OkHttpClient())
                android.util.Log.d("OkHttpNetworkClient", "Basic OkHttpClient set as fallback")
            }
            // 不再抛出异常，确保初始化完成
            android.util.Log.w("OkHttpNetworkClient", "OkHttpClient built with fallback, but original error was: ${e.message}")
        }
        
        // 验证初始化是否成功
        val finalClient = okHttpClientRef.get()
        val finalConfig = configRef.get()
        android.util.Log.d("OkHttpNetworkClient", "init() completed. client=${finalClient != null}, config=${finalConfig != null}")
        
        } catch (e: Exception) {
            android.util.Log.e("OkHttpNetworkClient", "init() failed with exception", e)
            // 即使出错，也尝试设置一个基本的客户端和配置，避免后续调用失败
            if (okHttpClientRef.get() == null) {
                try {
                    okHttpClientRef.set(OkHttpClient())
                    android.util.Log.w("OkHttpNetworkClient", "Set basic OkHttpClient after exception")
                } catch (_: Exception) {
                }
            }
            if (configRef.get() == null) {
                try {
                    configRef.set(config)
                    android.util.Log.w("OkHttpNetworkClient", "Set config after exception")
                } catch (_: Exception) {
                }
            }
            throw e
        }
    }

    override fun <T> execute(
        request: com.kernelflux.aether.network.api.Request,
        responseType: Class<T>,
        callback: NetworkCallback<T>
    ) {
        val client = okHttpClientRef.get()
        val config = configRef.get()

        android.util.Log.d("OkHttpNetworkClient", "execute() called, instance: ${this.hashCode()}, client=${client != null}, config=${config != null}")

        if (client == null || config == null) {
            android.util.Log.e("OkHttpNetworkClient", "execute() called but client not initialized. instance: ${this.hashCode()}, client=${client != null}, config=${config != null}")
            callback.onError(NetworkException.UnknownException("Network client not initialized. Please call init() first."))
            return
        }

        // 获取缓存策略
        val cacheStrategy = request.cacheStrategy ?: config.defaultCacheStrategy
        
        // 根据缓存策略处理请求
        when (cacheStrategy) {
            CacheStrategy.NO_CACHE -> {
                // 不使用缓存，直接请求网络
                val optimizedRequest = optimizeRequestForWeakNetwork(request, getNetworkState(), config)
                val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                executeWithRetry(client, okHttpRequest, optimizedRequest, responseType, callback, config)
            }
            
            CacheStrategy.CACHE_ONLY -> {
                // 仅使用缓存，如果缓存不存在则失败
                handleCacheResponse(request, responseType, callback, config) {
                    callback.onError(NetworkException.ConnectionException("Cache not available"))
                }
            }
            
            CacheStrategy.CACHE_FIRST -> {
                // 优先使用缓存，缓存不存在则请求网络
                val cacheResponse = getCacheResponse(request, responseType, config)
                if (cacheResponse != null) {
                    callback.onSuccess(cacheResponse)
                } else {
                    // 缓存不存在，请求网络
                    val optimizedRequest = optimizeRequestForWeakNetwork(request, getNetworkState(), config)
                    val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                    executeWithRetry(client, okHttpRequest, optimizedRequest, responseType, callback, config)
                }
            }
            
            CacheStrategy.NETWORK_FIRST -> {
                // 优先请求网络，失败则使用缓存
                val networkState = getNetworkState()
                if (networkState == NetworkState.NONE) {
                    // 无网络，尝试使用缓存
                    handleCacheResponse(request, responseType, callback, config) {
                        callback.onError(NetworkException.ConnectionException("No network and no cache available"))
                    }
                } else {
                    // 有网络，优先请求网络，失败则使用缓存
                    val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)
                    val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                    executeWithRetryAndCacheFallback(
                        client, okHttpRequest, optimizedRequest, responseType, callback, config
                    )
                }
            }
            
            CacheStrategy.CACHE_AND_NETWORK -> {
                // 同时请求网络和缓存，优先返回缓存，网络返回后更新缓存
                val cacheResponse = getCacheResponse(request, responseType, config)
                if (cacheResponse != null) {
                    // 先返回缓存
                    callback.onSuccess(cacheResponse)
                }
                
                // 同时请求网络（如果网络可用）
                val networkState = getNetworkState()
                if (networkState != NetworkState.NONE) {
                    val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)
                    val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                    executeWithRetry(client, okHttpRequest, optimizedRequest, responseType, object : NetworkCallback<T> {
                        override fun onSuccess(response: Response<T>) {
                            // 网络请求成功，更新缓存（如果之前返回了缓存，这里会再次回调）
                            // 为了避免重复回调，只在之前没有缓存时才回调
                            if (cacheResponse == null) {
                                callback.onSuccess(response)
                            }
                            // 注意：这里会更新缓存（在 convertResponse 中处理）
                        }
                        
                        override fun onError(exception: NetworkException) {
                            // 网络请求失败，如果之前没有返回缓存，则返回错误
                            if (cacheResponse == null) {
                                callback.onError(exception)
                            }
                        }
                    }, config)
                } else if (cacheResponse == null) {
                    // 无网络且无缓存，返回错误
                    callback.onError(NetworkException.ConnectionException("No network and no cache available"))
                }
            }
            
            CacheStrategy.CACHE_IF_AVAILABLE -> {
                // 仅当缓存过期时才请求网络
                val cache = config.cache
                if (cache != null) {
                    val cacheAdapter = OkHttpCacheAdapter(cache)
                    val cacheKey = cacheAdapter.keyGenerator.generate(request)
                    val cacheEntry = cache.get(cacheKey)
                    
                    if (cacheEntry != null && cacheEntry.isValid()) {
                        // 缓存有效，直接返回
                        val cacheResponse = getCacheResponse(request, responseType, config)
                        if (cacheResponse != null) {
                            callback.onSuccess(cacheResponse)
                            return
                        }
                    }
                }
                
                // 缓存不存在或已过期，请求网络
                val networkState = getNetworkState()
                if (networkState == NetworkState.NONE) {
                    // 无网络，尝试使用过期缓存
                    val cacheResponse = getCacheResponse(request, responseType, config)
                    if (cacheResponse != null) {
                        callback.onSuccess(cacheResponse)
                    } else {
                        callback.onError(NetworkException.ConnectionException("No network and no cache available"))
                    }
                } else {
                    val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)
                    val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                    executeWithRetry(client, okHttpRequest, optimizedRequest, responseType, callback, config)
                }
            }
        }
    }

    override fun <T> executeSync(
        request: com.kernelflux.aether.network.api.Request,
        responseType: Class<T>
    ): Response<T> {
        val client = okHttpClientRef.get()
        val config = configRef.get()

        if (client == null || config == null) {
            throw NetworkException.UnknownException("Network client not initialized")
        }

        // 获取缓存策略
        val cacheStrategy = request.cacheStrategy ?: config.defaultCacheStrategy
        val networkState = getNetworkState()

        // 根据缓存策略处理请求
        return when (cacheStrategy) {
            CacheStrategy.NO_CACHE -> {
                // 不使用缓存，直接请求网络
                val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)
                val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                try {
                    val okHttpResponse = client.newCall(okHttpRequest).execute()
                    convertResponse(okHttpResponse, optimizedRequest, responseType, config)
                } catch (e: SocketTimeoutException) {
                    throw NetworkException.TimeoutException("Request timeout", e)
                } catch (e: IOException) {
                    throw NetworkException.ConnectionException("Network error", e)
                }
            }
            
            CacheStrategy.CACHE_ONLY -> {
                // 仅使用缓存，如果缓存不存在则失败
                getCacheResponse(request, responseType, config)
                    ?: throw NetworkException.ConnectionException("Cache not available")
            }
            
            CacheStrategy.CACHE_FIRST -> {
                // 优先使用缓存，缓存不存在则请求网络
                val cacheResponse = getCacheResponse(request, responseType, config)
                if (cacheResponse != null) {
                    cacheResponse
                } else {
                    // 缓存不存在，请求网络
                    val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)
                    val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                    try {
                        val okHttpResponse = client.newCall(okHttpRequest).execute()
                        convertResponse(okHttpResponse, optimizedRequest, responseType, config)
                    } catch (e: SocketTimeoutException) {
                        throw NetworkException.TimeoutException("Request timeout", e)
                    } catch (e: IOException) {
                        throw NetworkException.ConnectionException("Network error", e)
                    }
                }
            }
            
            CacheStrategy.NETWORK_FIRST -> {
                // 优先请求网络，失败则使用缓存
                if (networkState == NetworkState.NONE) {
                    // 无网络，尝试使用缓存
                    getCacheResponse(request, responseType, config)
                        ?: throw NetworkException.ConnectionException("No network and no cache available")
                } else {
                    // 有网络，优先请求网络，失败则使用缓存
                    val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)
                    val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                    try {
                        val okHttpResponse = client.newCall(okHttpRequest).execute()
                        convertResponse(okHttpResponse, optimizedRequest, responseType, config)
                    } catch (e: SocketTimeoutException) {
                        // 超时，尝试使用缓存
                        getCacheResponse(request, responseType, config)
                            ?: throw NetworkException.TimeoutException("Request timeout", e)
                    } catch (e: IOException) {
                        // 网络错误，尝试使用缓存
                        getCacheResponse(request, responseType, config)
                            ?: throw NetworkException.ConnectionException("Network error", e)
                    }
                }
            }
            
            CacheStrategy.CACHE_AND_NETWORK -> {
                // 同时请求网络和缓存，优先返回缓存，网络返回后更新缓存
                val cacheResponse = getCacheResponse(request, responseType, config)
                if (cacheResponse != null) {
                    // 先返回缓存
                    cacheResponse
                } else {
                    // 无缓存，请求网络
                    if (networkState == NetworkState.NONE) {
                        throw NetworkException.ConnectionException("No network and no cache available")
                    }
                    val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)
                    val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                    try {
                        val okHttpResponse = client.newCall(okHttpRequest).execute()
                        convertResponse(okHttpResponse, optimizedRequest, responseType, config)
                    } catch (e: SocketTimeoutException) {
                        throw NetworkException.TimeoutException("Request timeout", e)
                    } catch (e: IOException) {
                        throw NetworkException.ConnectionException("Network error", e)
                    }
                }
            }
            
            CacheStrategy.CACHE_IF_AVAILABLE -> {
                // 仅当缓存过期时才请求网络
                val cache = config.cache
                if (cache != null) {
                    val cacheAdapter = OkHttpCacheAdapter(cache)
                    val cacheKey = cacheAdapter.keyGenerator.generate(request)
                    val cacheEntry = cache.get(cacheKey)
                    
                    if (cacheEntry != null && cacheEntry.isValid()) {
                        // 缓存有效，直接返回
                        val cacheResponse = getCacheResponse(request, responseType, config)
                        if (cacheResponse != null) {
                            return cacheResponse
                        }
                    }
                }
                
                // 缓存不存在或已过期，请求网络
                if (networkState == NetworkState.NONE) {
                    // 无网络，尝试使用过期缓存
                    val cacheResponse = getCacheResponse(request, responseType, config)
                    if (cacheResponse != null) {
                        cacheResponse
                    } else {
                        throw NetworkException.ConnectionException("No network and no cache available")
                    }
                } else {
                    val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)
                    val okHttpRequest = buildOkHttpRequest(optimizedRequest, config, cacheStrategy)
                    try {
                        val okHttpResponse = client.newCall(okHttpRequest).execute()
                        convertResponse(okHttpResponse, optimizedRequest, responseType, config)
                    } catch (e: SocketTimeoutException) {
                        throw NetworkException.TimeoutException("Request timeout", e)
                    } catch (e: IOException) {
                        throw NetworkException.ConnectionException("Network error", e)
                    }
                }
            }
        }
    }

    override fun cancel(tag: Any) {
        activeCalls[tag]?.forEach { it.cancel() }
        activeCalls.remove(tag)
    }

    override fun cancelAll() {
        activeCalls.values.flatten().forEach { it.cancel() }
        activeCalls.clear()
    }

    override fun getNetworkState(): NetworkState {
        return try {
            networkStateManagerRef.get()?.getCurrentState() ?: networkStateRef.get()
        } catch (e: Exception) {
            // 异常处理，返回缓存的状态
            networkStateRef.get()
        }
    }

    override fun addNetworkStateListener(listener: NetworkStateListener) {
        try {
            networkStateListeners.add(listener)
        } catch (e: Exception) {
            // 异常处理，避免崩溃
        }
    }

    override fun removeNetworkStateListener(listener: NetworkStateListener) {
        try {
            networkStateListeners.remove(listener)
        } catch (e: Exception) {
            // 异常处理，避免崩溃
        }
    }

    override fun getNetworkStateFlow(): Flow<NetworkState> {
        return try {
            networkStateManagerRef.get()?.getNetworkStateFlow()
                ?: flowOf(NetworkState.AVAILABLE)
        } catch (e: Exception) {
            // 异常处理，返回默认 Flow
            flowOf(NetworkState.AVAILABLE)
        }
    }

    // ========== 私有方法 ==========

    /**
     * 弱网优化请求（完整实现）
     */
    private fun optimizeRequestForWeakNetwork(
        request: com.kernelflux.aether.network.api.Request,
        networkState: NetworkState,
        config: NetworkConfig
    ): com.kernelflux.aether.network.api.Request {
        var optimizedRequest = request
        val weakHandler = config.weakNetworkHandler
        val requestOptimizer = config.requestOptimizer
        val romHandler = config.romCompatibilityHandler

        // 1. 获取网络质量指标和设备信息
        val metrics = weakHandler?.getNetworkMetrics() ?: com.kernelflux.aether.network.api.NetworkMetrics()
        val deviceInfo = weakHandler?.getDeviceInfo() ?: DeviceInfo()
        val isWeakNetwork = networkState == NetworkState.WEAK || weakHandler?.isWeakNetwork() == true

        // 2. 使用 RequestOptimizer 优化请求（如果提供）
        if (requestOptimizer != null && isWeakNetwork) {
            optimizedRequest = requestOptimizer.optimize(optimizedRequest, metrics, deviceInfo)
            
            // 2.1 应用超时优化
            val optimizedTimeout = requestOptimizer.getOptimizedTimeout(
                optimizedRequest,
                metrics,
                optimizedRequest.timeout ?: config.connectTimeout
            )
            optimizedRequest = optimizedRequest.copy(timeout = optimizedTimeout)
            
            // 2.2 应用请求降级
            if (requestOptimizer.shouldDegradeRequest(optimizedRequest, metrics)) {
                optimizedRequest = requestOptimizer.degradeRequest(optimizedRequest)
            }
        }

        // 3. 使用 WeakNetworkHandler 优化请求
        if (isWeakNetwork && weakHandler != null) {
            optimizedRequest = weakHandler.optimizeRequest(optimizedRequest)
            
            // 3.1 应用超时调整
            if (weakHandler.shouldReduceTimeout()) {
                val weakNetworkTimeout = weakHandler.getWeakNetworkTimeout()
                optimizedRequest = optimizedRequest.copy(timeout = weakNetworkTimeout)
            }
        }

        // 4. 添加压缩支持（优先使用 RequestOptimizer，否则使用 WeakNetworkHandler）
        val shouldCompress = if (requestOptimizer != null && isWeakNetwork) {
            requestOptimizer.shouldCompressRequest(optimizedRequest, metrics)
        } else {
            weakHandler?.shouldCompressRequest() == true
        }
        
        if (shouldCompress) {
            val algorithm = weakHandler?.getCompressionAlgorithm() ?: CompressionAlgorithm.GZIP
            val acceptEncoding = when (algorithm) {
                CompressionAlgorithm.GZIP -> "gzip, deflate"
                CompressionAlgorithm.BROTLI -> "br, gzip, deflate"
                CompressionAlgorithm.DEFLATE -> "deflate"
                CompressionAlgorithm.NONE -> ""
            }
            if (acceptEncoding.isNotEmpty()) {
                optimizedRequest = optimizedRequest.copy(
                    headers = optimizedRequest.headers + mapOf(
                        "Accept-Encoding" to acceptEncoding
                    )
                )
            }
        }

        // 5. ROM 兼容性处理
        if (romHandler != null) {
            optimizedRequest = romHandler.handleBackgroundRestriction(
                optimizedRequest,
                deviceInfo.isBackground
            )
        }

        // 6. 添加弱网标识头（用于服务端识别）
        if (isWeakNetwork) {
            val qualityScore = weakHandler?.getNetworkQuality() ?: 50
            val networkQualityHeader = mapOf(
                "X-Network-Quality" to qualityScore.toString(),
                "X-Device-Performance" to if (deviceInfo.isLowEndDevice) "low" else "normal",
                "X-Network-State" to networkState.name
            )
            optimizedRequest = optimizedRequest.copy(
                headers = optimizedRequest.headers + networkQualityHeader
            )
        }

        return optimizedRequest
    }

    private fun <T> executeWithRetry(
        client: OkHttpClient,
        okHttpRequest: Request,
        request: com.kernelflux.aether.network.api.Request,
        responseType: Class<T>,
        callback: NetworkCallback<T>,
        config: NetworkConfig
    ) {
        val retryStrategy = request.retryStrategy ?: config.defaultRetryStrategy
        val tag = request.tag ?: Any()

        // 保存 call 引用以便取消
        val calls = activeCalls.getOrPut(tag) { mutableListOf() }

        fun executeRequest(retryCount: Int) {
            val call = client.newCall(okHttpRequest)
            calls.add(call)

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    calls.remove(call)

                    val exception = when (e) {
                        is SocketTimeoutException -> NetworkException.TimeoutException(
                            "Request timeout",
                            e
                        )

                        else -> NetworkException.ConnectionException("Network error", e)
                    }

                    // 检查是否需要重试
                    val apiRequest = request
                    if (retryStrategy != null && retryStrategy.shouldRetry(
                            apiRequest,
                            null,
                            exception,
                            retryCount
                        )
                    ) {
                        val delay = retryStrategy.getRetryDelay(retryCount)
                        // 延迟重试（这里简化处理，实际应该使用 Handler 或协程）
                        Thread.sleep(delay)
                        executeRequest(retryCount + 1)
                    } else {
                        callback.onError(exception)
                    }
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    calls.remove(call)

                    try {
                        val apiResponse = convertResponse(response, request, responseType, config)

                        // 检查是否需要重试（HTTP 错误）
                        val apiRequest = request
                        if (!apiResponse.isSuccessful && retryStrategy != null) {
                            val exception = NetworkException.HttpException(
                                apiResponse.code,
                                apiResponse.message,
                                apiResponse
                            )

                            if (retryStrategy.shouldRetry(
                                    apiRequest,
                                    apiResponse,
                                    exception,
                                    retryCount
                                )
                            ) {
                                val delay = retryStrategy.getRetryDelay(retryCount)
                                Thread.sleep(delay)
                                executeRequest(retryCount + 1)
                                return
                            }
                        }

                        callback.onSuccess(apiResponse)
                    } catch (e: NetworkException) {
                        callback.onError(e)
                    } catch (e: Exception) {
                        callback.onError(NetworkException.ParseException("Parse error", e))
                    } finally {
                        // 确保 Response 被关闭
                        response.close()
                    }
                }
            })
        }

        executeRequest(0)
    }

    private fun buildOkHttpRequest(
        request: com.kernelflux.aether.network.api.Request,
        config: NetworkConfig,
        cacheStrategy: CacheStrategy = CacheStrategy.NO_CACHE
    ): Request {
        val fullUrl = request.buildUrl(config.baseUrl)
        val urlBuilder = fullUrl.toHttpUrlOrNull()?.newBuilder()
            ?: throw NetworkException.UnknownException("Invalid URL: $fullUrl")

        val builder = Request.Builder()
            .url(urlBuilder.build())

        // 添加请求头
        val allHeaders = NetworkUtils.mergeHeaders(config.defaultHeaders, request.headers)
        allHeaders.forEach { (key, value) ->
            builder.addHeader(key, value)
        }

        // 设置请求体
        val requestBody = buildRequestBody(request, config)
        when (request.method) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> {
                val body = requestBody ?: ByteArray(0).toRequestBody(null)
                builder.post(body)
            }

            HttpMethod.PUT -> {
                val body = requestBody ?: ByteArray(0).toRequestBody(null)
                builder.put(body)
            }

            HttpMethod.DELETE -> {
                builder.delete(requestBody)
            }

            HttpMethod.PATCH -> {
                val body = requestBody ?: ByteArray(0).toRequestBody(null)
                builder.patch(body)
            }

            HttpMethod.HEAD -> builder.head()
            HttpMethod.OPTIONS -> builder.method("OPTIONS", requestBody)
        }

        // 设置标签
        request.tag?.let { builder.tag(it) }
        
        // 根据缓存策略设置 Cache-Control 头
        when (cacheStrategy) {
            CacheStrategy.NO_CACHE -> {
                builder.cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
            }
            CacheStrategy.CACHE_ONLY -> {
                builder.cacheControl(okhttp3.CacheControl.FORCE_CACHE)
            }
            CacheStrategy.CACHE_FIRST -> {
                // 允许使用缓存，但优先网络
                builder.cacheControl(okhttp3.CacheControl.Builder()
                    .maxAge(0, TimeUnit.SECONDS)
                    .build())
            }
            CacheStrategy.NETWORK_FIRST -> {
                // 优先网络，但允许缓存
                builder.cacheControl(okhttp3.CacheControl.Builder()
                    .maxAge(60, TimeUnit.SECONDS)
                    .build())
            }
            CacheStrategy.CACHE_AND_NETWORK -> {
                // 允许缓存和网络
                builder.cacheControl(okhttp3.CacheControl.Builder()
                    .maxAge(60, TimeUnit.SECONDS)
                    .build())
            }
            CacheStrategy.CACHE_IF_AVAILABLE -> {
                // 仅当缓存过期时才请求网络
                builder.cacheControl(okhttp3.CacheControl.Builder()
                    .maxStale(Int.MAX_VALUE, TimeUnit.SECONDS)
                    .build())
            }
        }

        return builder.build()
    }

    private fun buildRequestBody(
        request: com.kernelflux.aether.network.api.Request,
        config: NetworkConfig
    ): RequestBody? {
        val body = request.body ?: return null

        return when (body) {
            is com.kernelflux.aether.network.api.RequestBody.JsonBody -> {
                val converter = request.dataConverter ?: config.defaultDataConverter
                val contentType = converter?.getContentType() ?: "application/json; charset=utf-8"
                val json = if (converter != null) {
                    String(converter.toBytes(body.data))
                } else {
                    gson.toJson(body.data)
                }
                json.toRequestBody(contentType.toMediaType())
            }
            
            is com.kernelflux.aether.network.api.RequestBody.ProtobufBody -> {
                val converter = request.dataConverter ?: config.defaultDataConverter
                val contentType = converter?.getContentType() ?: "application/x-protobuf"
                val protobufBytes = if (converter != null) {
                    // 如果提供了转换器，使用转换器（支持 Message 对象）
                    converter.toBytes(body.data)
                } else {
                    // 如果没有转换器，直接使用字节数组
                    body.data
                }
                protobufBytes.toRequestBody(contentType.toMediaType())
            }

            is com.kernelflux.aether.network.api.RequestBody.FormBody -> {
                val formBuilder = FormBody.Builder()
                body.fields.forEach { (key, value) ->
                    formBuilder.add(key, value)
                }
                formBuilder.build()
            }

            is com.kernelflux.aether.network.api.RequestBody.MultipartBody -> {
                val multipartBuilder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)

                body.parts.forEach { part ->
                    val partBody = when {
                        part.content != null -> {
                            val contentType = part.contentType?.toMediaTypeOrNull()
                                ?: "application/octet-stream".toMediaType()
                            part.content!!.toRequestBody(contentType)
                        }

                        part.value != null -> {
                            val contentType = part.contentType?.toMediaTypeOrNull()
                                ?: "text/plain".toMediaType()
                            part.value!!.toRequestBody(contentType)
                        }

                        else -> {
                            "".toRequestBody(null)
                        }
                    }

                    if (part.filename != null) {
                        multipartBuilder.addFormDataPart(part.name, part.filename!!, partBody)
                    } else {
                        multipartBuilder.addFormDataPart(part.name, null, partBody)
                    }
                }

                multipartBuilder.build()
            }

            is com.kernelflux.aether.network.api.RequestBody.RawBody -> {
                val contentType = body.contentType.toMediaTypeOrNull()
                    ?: "application/octet-stream".toMediaType()
                body.data.toRequestBody(contentType)
            }

            is com.kernelflux.aether.network.api.RequestBody.StreamBody -> {
                // OkHttp 4.x 支持 InputStream，但需要知道 contentLength
                // 如果 contentLength 为 null，需要先读取到 ByteArray
                val contentType = body.contentType.toMediaTypeOrNull()
                    ?: "application/octet-stream".toMediaType()
                if (body.contentLength != null && body.contentLength!! > 0) {
                    // 有明确的长度，使用 RequestBody.create(MediaType, Long, InputStream)
                    // 但 OkHttp 4.x 的 API 可能不同，这里先读取到 ByteArray
                    val bytes = body.inputStream.readBytes()
                    bytes.toRequestBody(contentType)
                } else {
                    // 没有长度信息，先读取到 ByteArray
                    val bytes = body.inputStream.readBytes()
                    bytes.toRequestBody(contentType)
                }
            }

            is com.kernelflux.aether.network.api.RequestBody.EmptyBody -> null
        }
    }

    private fun <T> convertResponse(
        okHttpResponse: okhttp3.Response,
        request: com.kernelflux.aether.network.api.Request,
        responseType: Class<T>,
        config: NetworkConfig
    ): Response<T> {
        val code = okHttpResponse.code
        val message = okHttpResponse.message
        val headers = okHttpResponse.headers.toMultimap()
        val isFromCache = okHttpResponse.cacheResponse != null

        // 重要：OkHttp 的 ResponseBody 只能读取一次，读取后会自动关闭
        // 我们需要先读取字节数组，然后再进行后续处理
        val responseBody = okHttpResponse.body
        val bodyBytes = try {
            responseBody?.bytes() ?: ByteArray(0)
        } catch (e: IOException) {
            // 读取失败，返回空数组
            ByteArray(0)
        } finally {
            // 确保 ResponseBody 被关闭
            responseBody?.close()
        }

        // 如果响应体为空且不是成功响应，直接返回错误响应
        if (bodyBytes.isEmpty() && code !in 200..299) {
            return Response(
                data = null,
                code = code,
                message = message,
                headers = headers,
                request = request,
                isFromCache = isFromCache,
                isSuccessful = false
            )
        }

        // 解密
        val decryptedBody = try {
            request.decryptor?.decrypt(bodyBytes) ?: bodyBytes
        } catch (e: Exception) {
            // 解密失败，返回原始数据
            bodyBytes
        }

        // 数据转换
        val converter = request.dataConverter ?: config.defaultDataConverter
        val data = try {
            if (decryptedBody.isEmpty()) {
                null
            } else if (converter != null) {
                // 使用指定的转换器
                converter.fromBytes(decryptedBody, responseType)
            } else {
                // 默认使用 Gson（JSON）
                val jsonString = String(decryptedBody, Charsets.UTF_8)
                if (jsonString.isBlank()) {
                    null
                } else {
                    gson.fromJson(jsonString, responseType)
                }
            }
        } catch (e: Exception) {
            // 转换失败，返回 null
            null
        }

        val response = Response(
            data = data,
            code = code,
            message = message,
            headers = headers,
            request = request,
            isFromCache = isFromCache,
            isSuccessful = code in 200..299 && data != null
        )
        
        // 保存响应到缓存（如果策略允许且响应成功）
        if (!isFromCache && response.isSuccessful && data != null) {
            val cacheStrategy = request.cacheStrategy ?: config.defaultCacheStrategy
            if (cacheStrategy != CacheStrategy.NO_CACHE && cacheStrategy != CacheStrategy.CACHE_ONLY) {
                try {
                    saveResponseToCache(request, okHttpResponse, config, decryptedBody)
                } catch (e: Exception) {
                    // 缓存保存失败，不影响响应返回
                    android.util.Log.w("OkHttpNetworkClient", "Failed to save response to cache", e)
                }
            }
        }
        
        return response
    }
    
    /**
     * 保存响应到缓存
     */
    private fun saveResponseToCache(
        request: com.kernelflux.aether.network.api.Request,
        okHttpResponse: okhttp3.Response,
        config: NetworkConfig,
        bodyBytes: ByteArray
    ) {
        val cache = config.cache ?: return
        val cacheAdapter = OkHttpCacheAdapter(cache)
        val cacheKey = cacheAdapter.keyGenerator.generate(request)
        
        val headers = okHttpResponse.headers.toMultimap()
        val ttl = getCacheTTL(okHttpResponse)
        
        val entry = CacheEntry(
            data = bodyBytes,
            headers = headers,
            timestamp = System.currentTimeMillis(),
            ttl = ttl,
            etag = okHttpResponse.header("ETag"),
            lastModified = okHttpResponse.header("Last-Modified")
        )
        
        cache.put(cacheKey, entry)
    }
    
    /**
     * 从响应头获取缓存 TTL
     */
    private fun getCacheTTL(response: okhttp3.Response): Long {
        val cacheControl = response.cacheControl
        if (cacheControl.maxAgeSeconds > 0) {
            return cacheControl.maxAgeSeconds * 1000L
        }
        
        // 从 Expires 头获取
        val expires = response.header("Expires")
        if (expires != null) {
            // 解析 Expires 头（简化处理，默认 1 小时）
            return 3600_000L
        }
        
        // 默认 TTL：1 小时
        return 3600_000L
    }

    /**
     * 处理缓存响应
     * @param onCacheNotFound 缓存不存在时的回调
     */
    private fun <T> handleCacheResponse(
        request: com.kernelflux.aether.network.api.Request,
        responseType: Class<T>,
        callback: NetworkCallback<T>,
        config: NetworkConfig,
        onCacheNotFound: () -> Unit = {}
    ) {
        val cache = config.cache ?: return
        val cacheAdapter = OkHttpCacheAdapter(cache)
        val cacheKey = cacheAdapter.keyGenerator.generate(request)

        val cacheEntry = cache.get(cacheKey)
        if (cacheEntry != null && cacheEntry.isValid()) {
            try {
                val converter = request.dataConverter ?: config.defaultDataConverter
                val data = if (converter != null) {
                    converter.fromBytes(cacheEntry.data, responseType)
                } else {
                    val jsonString = String(cacheEntry.data, Charsets.UTF_8)
                    gson.fromJson(jsonString, responseType)
                }

                val response = Response(
                    data = data,
                    code = 200,
                    message = "OK",
                    headers = cacheEntry.headers,
                    request = request,
                    isFromCache = true,
                    isSuccessful = true
                )

                callback.onSuccess(response)
            } catch (e: Exception) {
                callback.onError(NetworkException.ParseException("Cache parse error", e))
            }
        } else {
            onCacheNotFound()
        }
    }
    
    /**
     * 执行请求（带重试和缓存降级）
     * 网络请求失败时，尝试使用缓存
     */
    private fun <T> executeWithRetryAndCacheFallback(
        client: OkHttpClient,
        okHttpRequest: Request,
        request: com.kernelflux.aether.network.api.Request,
        responseType: Class<T>,
        callback: NetworkCallback<T>,
        config: NetworkConfig
    ) {
        val retryStrategy = request.retryStrategy ?: config.defaultRetryStrategy
        val tag = request.tag ?: Any()

        // 保存 call 引用以便取消
        val calls = activeCalls.getOrPut(tag) { mutableListOf() }

        fun executeRequest(retryCount: Int) {
            val call = client.newCall(okHttpRequest)
            calls.add(call)

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    calls.remove(call)

                    val exception = when (e) {
                        is SocketTimeoutException -> NetworkException.TimeoutException(
                            "Request timeout",
                            e
                        )
                        else -> NetworkException.ConnectionException("Network error", e)
                    }

                    // 检查是否需要重试
                    val apiRequest = request
                    if (retryStrategy != null && retryStrategy.shouldRetry(
                            apiRequest,
                            null,
                            exception,
                            retryCount
                        )
                    ) {
                        val delay = retryStrategy.getRetryDelay(retryCount)
                        Thread.sleep(delay)
                        executeRequest(retryCount + 1)
                    } else {
                        // 重试失败，尝试使用缓存
                        val cacheResponse = getCacheResponse(request, responseType, config)
                        if (cacheResponse != null) {
                            callback.onSuccess(cacheResponse)
                        } else {
                            callback.onError(exception)
                        }
                    }
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    calls.remove(call)

                    try {
                        val apiResponse = convertResponse(response, request, responseType, config)

                        // 检查是否需要重试（HTTP 错误）
                        val apiRequest = request
                        if (!apiResponse.isSuccessful && retryStrategy != null) {
                            val exception = NetworkException.HttpException(
                                apiResponse.code,
                                apiResponse.message,
                                apiResponse
                            )

                            if (retryStrategy.shouldRetry(
                                    apiRequest,
                                    apiResponse,
                                    exception,
                                    retryCount
                                )
                            ) {
                                val delay = retryStrategy.getRetryDelay(retryCount)
                                Thread.sleep(delay)
                                executeRequest(retryCount + 1)
                                return
                            }
                        }

                        callback.onSuccess(apiResponse)
                    } catch (e: NetworkException) {
                        // 网络请求失败，尝试使用缓存
                        val cacheResponse = getCacheResponse(request, responseType, config)
                        if (cacheResponse != null) {
                            callback.onSuccess(cacheResponse)
                        } else {
                            callback.onError(e)
                        }
                    } catch (e: Exception) {
                        // 解析错误，尝试使用缓存
                        val cacheResponse = getCacheResponse(request, responseType, config)
                        if (cacheResponse != null) {
                            callback.onSuccess(cacheResponse)
                        } else {
                            callback.onError(NetworkException.ParseException("Parse error", e))
                        }
                    } finally {
                        // 确保 Response 被关闭
                        response.close()
                    }
                }
            })
        }

        executeRequest(0)
    }

    private fun <T> getCacheResponse(
        request: com.kernelflux.aether.network.api.Request,
        responseType: Class<T>,
        config: NetworkConfig
    ): Response<T>? {
        val cache = config.cache ?: return null
        val cacheAdapter = OkHttpCacheAdapter(cache)
        val cacheKey = cacheAdapter.keyGenerator.generate(request)

        val cacheEntry = cache.get(cacheKey) ?: return null
        if (!cacheEntry.isValid()) return null

        return try {
            val converter = request.dataConverter ?: config.defaultDataConverter
            val data = if (converter != null) {
                converter.fromBytes(cacheEntry.data, responseType)
            } else {
                val jsonString = String(cacheEntry.data, Charsets.UTF_8)
                gson.fromJson(jsonString, responseType)
            }

            Response(
                data = data,
                code = 200,
                message = "OK",
                headers = cacheEntry.headers,
                request = request,
                isFromCache = true,
                isSuccessful = true
            )
        } catch (e: Exception) {
            null
        }
    }
}
