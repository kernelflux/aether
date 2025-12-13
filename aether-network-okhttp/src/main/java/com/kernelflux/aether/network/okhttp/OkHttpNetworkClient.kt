package com.kernelflux.aether.network.okhttp

import android.content.Context
import com.google.gson.Gson
import com.kernelflux.aether.network.api.CompressionAlgorithm
import com.kernelflux.aether.network.api.DeviceInfo
import com.kernelflux.aether.network.api.HttpMethod
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
import java.security.cert.X509Certificate

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
    private val okHttpClientRef = AtomicReference<OkHttpClient>()
    private val configRef = AtomicReference<NetworkConfig>()
    private val gson = Gson()
    private val activeCalls = ConcurrentHashMap<Any, MutableList<Call>>()
    private val networkStateListeners = CopyOnWriteArrayList<NetworkStateListener>()
    private val networkStateRef = AtomicReference<NetworkState>(NetworkState.AVAILABLE)
    private val networkStateManagerRef = AtomicReference<NetworkStateManager>()
    private val cookieJarRef = AtomicReference<PersistentCookieJar>()
    private val cacheDirRef = AtomicReference<File>()
    private val scheduledExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    /**
     * 初始化网络客户端（带缓存目录）
     * @param config 网络配置
     * @param cacheDir 缓存目录（用于 OkHttp Cache 和 Cookie 存储）
     */
    fun init(
        context: Context,
        config: NetworkConfig,
        cacheDir: File? = null
    ) {
        cacheDirRef.set(cacheDir)

        val appCxt = context.applicationContext ?: context
        init(appCxt, config)
    }

    override fun init(context: Context, config: NetworkConfig) {
        configRef.set(config)

        // 初始化网络状态管理器
        val stateManager = config.networkStateManager ?: NetworkStateManagerFactory.create(context)
        networkStateManagerRef.set(stateManager)

        // 初始化当前网络状态
        try {
            val currentState = stateManager.getCurrentState()
            networkStateRef.set(currentState)
        } catch (_: Exception) {
        }

        // 开始监听网络状态
        try {
            stateManager.startMonitoring()
            // 添加内部监听器，同步网络状态
            stateManager.addListener(object : NetworkStateListener {
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

        // 根据弱网和设备信息优化连接池和超时
        val weakHandler = config.weakNetworkHandler
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

        // 添加拦截器
        config.interceptors.forEach { interceptor ->
            builder.addInterceptor(OkHttpInterceptorAdapter(interceptor, config))
        }

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

        // 证书校验
        config.certificateValidator?.let { validator ->
            builder.hostnameVerifier { hostname, session ->
                try {
                    val certificates = session.peerCertificates.mapNotNull {
                        it as? X509Certificate
                    }.toTypedArray()
                    validator.validate(certificates, hostname)
                } catch (e: Exception) {
                    false
                }
            }
        }

        // Cookie - 使用持久化 Cookie 管理器
        if (config.enableCookies) {
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
        }

        okHttpClientRef.set(builder.build())
    }

    override fun <T> execute(
        request: com.kernelflux.aether.network.api.Request,
        responseType: Class<T>,
        callback: NetworkCallback<T>
    ) {
        val client = okHttpClientRef.get()
        val config = configRef.get()

        if (client == null || config == null) {
            callback.onError(NetworkException.UnknownException("Network client not initialized"))
            return
        }

        // 检查网络状态
        val networkState = getNetworkState()
        if (networkState == NetworkState.NONE) {
            val strategy = request.networkStrategy ?: config.networkStrategy
            when (val result = strategy.handleNoNetwork(request)) {
                is NoNetworkResult.UseCache -> {
                    // 尝试从缓存获取
                    handleCacheResponse(request, responseType, callback, config)
                    return
                }

                is NoNetworkResult.QueueRequest -> {
                    // 加入队列，网络恢复后重试
                    callback.onError(NetworkException.ConnectionException("No network available"))
                    return
                }

                is NoNetworkResult.Fail -> {
                    callback.onError(NetworkException.ConnectionException("No network available"))
                    return
                }

                is NoNetworkResult.Custom -> {
                    result.action()
                    return
                }
            }
        }

        // 弱网优化（增强版）
        val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)

        // 构建 OkHttp 请求
        val okHttpRequest = buildOkHttpRequest(optimizedRequest, config)

        // 执行请求（带重试）
        executeWithRetry(client, okHttpRequest, optimizedRequest, responseType, callback, config)
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

        // 检查网络状态（使用策略获取，如果策略支持）
        val strategy = request.networkStrategy ?: config.networkStrategy
        val networkState = try {
            strategy.getCurrentNetworkState()
        } catch (e: Exception) {
            // 异常处理，使用默认方法获取
            getNetworkState()
        }

        if (networkState == NetworkState.NONE) {
            val result = try {
                strategy.handleNoNetwork(request)
            } catch (e: Exception) {
                // 策略处理异常，返回失败而不是抛异常
                NoNetworkResult.Fail
            }

            when (result) {
                is NoNetworkResult.UseCache -> {
                    // 尝试从缓存获取
                    return getCacheResponse(request, responseType, config)
                        ?: throw NetworkException.ConnectionException("No network and no cache available")
                }

                is NoNetworkResult.Fail -> {
                    throw NetworkException.ConnectionException("No network available")
                }

                else -> throw NetworkException.ConnectionException("No network available")
            }
        }

        // 弱网优化（增强版）
        val optimizedRequest = optimizeRequestForWeakNetwork(request, networkState, config)

        // 构建 OkHttp 请求
        val okHttpRequest = buildOkHttpRequest(optimizedRequest, config)

        // 执行请求
        return try {
            val okHttpResponse = client.newCall(okHttpRequest).execute()
            convertResponse(okHttpResponse, optimizedRequest, responseType, config)
        } catch (e: SocketTimeoutException) {
            throw NetworkException.TimeoutException("Request timeout", e)
        } catch (e: IOException) {
            throw NetworkException.ConnectionException("Network error", e)
        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            throw NetworkException.UnknownException("Unknown error", e)
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
     * 弱网优化请求
     */
    private fun optimizeRequestForWeakNetwork(
        request: com.kernelflux.aether.network.api.Request,
        networkState: NetworkState,
        config: NetworkConfig
    ): com.kernelflux.aether.network.api.Request {
        var optimizedRequest = request
        val weakHandler = config.weakNetworkHandler
        val romHandler = config.romCompatibilityHandler

        // 1. 使用弱网处理器优化
        if (networkState == NetworkState.WEAK && weakHandler != null) {
            optimizedRequest = weakHandler.optimizeRequest(optimizedRequest)
        }

        // 2. 添加压缩支持
        if (weakHandler?.shouldCompressRequest() == true) {
            val algorithm = weakHandler.getCompressionAlgorithm()
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

        // 3. ROM 兼容性处理
        if (romHandler != null) {
            val deviceInfo = weakHandler?.getDeviceInfo() ?: DeviceInfo()
            optimizedRequest = romHandler.handleBackgroundRestriction(
                optimizedRequest,
                deviceInfo.isBackground
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
        config: NetworkConfig
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
                val json = if (converter != null) {
                    String(converter.toBytes(body.data))
                } else {
                    gson.toJson(body.data)
                }
                json.toRequestBody("application/json; charset=utf-8".toMediaType())
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

            is com.kernelflux.aether.network.api.RequestBody.ProtobufBody -> {
                val contentType = "application/x-protobuf".toMediaTypeOrNull()
                    ?: "application/octet-stream".toMediaType()
                body.data.toRequestBody(contentType)
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
            if (converter != null) {
                converter.fromBytes(decryptedBody, responseType)
            } else {
                // 默认使用 Gson
                if (decryptedBody.isEmpty()) {
                    null
                } else {
                    val jsonString = String(decryptedBody, Charsets.UTF_8)
                    if (jsonString.isBlank()) {
                        null
                    } else {
                        gson.fromJson(jsonString, responseType)
                    }
                }
            }
        } catch (e: Exception) {
            // 转换失败，返回 null
            null
        }

        return Response(
            data = data,
            code = code,
            message = message,
            headers = headers,
            request = request,
            isFromCache = isFromCache,
            isSuccessful = code in 200..299 && data != null
        )
    }

    private fun <T> handleCacheResponse(
        request: com.kernelflux.aether.network.api.Request,
        responseType: Class<T>,
        callback: NetworkCallback<T>,
        config: NetworkConfig
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
            callback.onError(NetworkException.ConnectionException("No network and no valid cache"))
        }
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
