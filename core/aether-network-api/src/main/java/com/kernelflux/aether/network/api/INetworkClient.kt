package com.kernelflux.aether.network.api

import android.content.Context

/**
 * 网络客户端接口
 * 支持多种 HTTP 框架实现（OkHttp、Retrofit 等）
 *
 * @author Aether Framework
 */
interface INetworkClient {

    /**
     * 初始化网络客户端
     */
    fun init(context: Context, config: NetworkConfig)

    /**
     * 异步执行请求
     * @param request 请求对象
     * @param responseType 响应类型
     * @param callback 回调
     */
    fun <T> execute(
        request: Request,
        responseType: Class<T>,
        callback: NetworkCallback<T>
    )

    /**
     * 同步执行请求
     * @param request 请求对象
     * @param responseType 响应类型
     * @return 响应对象
     */
    fun <T> executeSync(
        request: Request,
        responseType: Class<T>
    ): Response<T>

    /**
     * 取消请求
     * @param tag 请求标签
     */
    fun cancel(tag: Any)

    /**
     * 取消所有请求
     */
    fun cancelAll()

    /**
     * 获取网络状态
     */
    fun getNetworkState(): NetworkState

    /**
     * 添加网络状态监听器
     * @param listener 监听器
     */
    fun addNetworkStateListener(listener: NetworkStateListener)

    /**
     * 移除网络状态监听器
     * @param listener 监听器
     */
    fun removeNetworkStateListener(listener: NetworkStateListener)

    /**
     * 获取网络状态 Flow
     * 使用 Kotlin Flow 提供响应式网络状态
     * @return 网络状态 Flow
     */
    fun getNetworkStateFlow(): kotlinx.coroutines.flow.Flow<NetworkState>
}

/**
 * 网络请求回调
 */
interface NetworkCallback<T> {
    /**
     * 成功回调
     */
    fun onSuccess(response: Response<T>)

    /**
     * 失败回调
     */
    fun onError(exception: NetworkException)
}

/**
 * 请求构建器 DSL
 * 使用 Kotlin 语法糖提供简洁的 API
 */
class RequestBuilder {
    private var url: String = ""
    private var method: HttpMethod = HttpMethod.GET
    private var headers: MutableMap<String, String> = mutableMapOf()
    private var params: MutableMap<String, Any> = mutableMapOf()
    private var body: RequestBody? = null
    private var tag: Any? = null
    private var timeout: Long? = null
    private var retryStrategy: RetryStrategy? = null
    private var cacheStrategy: CacheStrategy? = null
    private var encryptor: Encryptor? = null
    private var decryptor: Decryptor? = null
    private var dataConverter: DataConverter? = null
    private var weakNetworkHandler: WeakNetworkHandler? = null
    private var networkStrategy: NetworkStrategy? = null

    /**
     * 设置 URL
     */
    fun url(url: String) = apply { this.url = url }

    /**
     * 设置请求方法
     */
    fun method(method: HttpMethod) = apply { this.method = method }

    /**
     * 添加请求头
     */
    fun header(name: String, value: String) = apply { this.headers[name] = value }

    /**
     * 添加多个请求头
     */
    fun headers(headers: Map<String, String>) = apply { this.headers.putAll(headers) }

    /**
     * 添加查询参数
     */
    fun param(name: String, value: Any) = apply { this.params[name] = value }

    /**
     * 添加多个查询参数
     */
    fun params(params: Map<String, Any>) = apply { this.params.putAll(params) }

    /**
     * 设置请求体
     */
    fun body(body: RequestBody) = apply { this.body = body }

    /**
     * 设置 JSON 请求体
     */
    fun jsonBody(data: Any) = apply { this.body = RequestBody.JsonBody(data) }

    /**
     * 设置表单请求体
     */
    fun formBody(fields: Map<String, String>) = apply {
        this.body = RequestBody.FormBody(fields)
    }

    /**
     * 设置多部分表单请求体
     */
    fun multipartBody(parts: List<RequestBody.MultipartBody.Part>) = apply {
        this.body = RequestBody.MultipartBody(parts)
    }

    /**
     * 设置原始字节流请求体
     */
    fun rawBody(data: ByteArray, contentType: String) = apply {
        this.body = RequestBody.RawBody(data, contentType)
    }

    /**
     * 设置 Protobuf 请求体
     */
    fun protobufBody(data: ByteArray) = apply {
        this.body = RequestBody.ProtobufBody(data)
    }

    /**
     * 设置请求标签（用于取消请求）
     */
    fun tag(tag: Any) = apply { this.tag = tag }

    /**
     * 设置超时时间（毫秒）
     */
    fun timeout(timeout: Long) = apply { this.timeout = timeout }

    /**
     * 设置重试策略
     */
    fun retryStrategy(strategy: RetryStrategy) = apply { this.retryStrategy = strategy }

    /**
     * 设置缓存策略
     */
    fun cacheStrategy(strategy: CacheStrategy) = apply { this.cacheStrategy = strategy }

    /**
     * 设置加密器
     */
    fun encryptor(encryptor: Encryptor) = apply { this.encryptor = encryptor }

    /**
     * 设置解密器
     */
    fun decryptor(decryptor: Decryptor) = apply { this.decryptor = decryptor }

    /**
     * 设置数据转换器
     */
    fun dataConverter(converter: DataConverter) = apply { this.dataConverter = converter }

    /**
     * 设置弱网处理器
     */
    fun weakNetworkHandler(handler: WeakNetworkHandler) = apply {
        this.weakNetworkHandler = handler
    }

    /**
     * 设置网络策略
     */
    fun networkStrategy(strategy: NetworkStrategy) = apply {
        this.networkStrategy = strategy
    }

    /**
     * 构建请求对象
     */
    fun build(): Request {
        return Request(
            url = url,
            method = method,
            headers = headers,
            params = params,
            body = body,
            tag = tag,
            timeout = timeout,
            retryStrategy = retryStrategy,
            cacheStrategy = cacheStrategy,
            encryptor = encryptor,
            decryptor = decryptor,
            dataConverter = dataConverter,
            weakNetworkHandler = weakNetworkHandler,
            networkStrategy = networkStrategy
        )
    }

    companion object {
        /**
         * 创建请求构建器
         */
        fun create() = RequestBuilder()
    }
}

/**
 * GET 请求扩展函数
 */
inline fun <reified T> INetworkClient.get(
    url: String,
    callback: NetworkCallback<T>,
    noinline block: RequestBuilder.() -> Unit = {}
) {
    val request = RequestBuilder.create()
        .url(url)
        .method(HttpMethod.GET)
        .apply(block)
        .build()

    execute(request, T::class.java, callback)
}

/**
 * POST 请求扩展函数
 */
inline fun <reified T> INetworkClient.post(
    url: String,
    callback: NetworkCallback<T>,
    noinline block: RequestBuilder.() -> Unit = {}
) {
    val request = RequestBuilder.create()
        .url(url)
        .method(HttpMethod.POST)
        .apply(block)
        .build()

    execute(request, T::class.java, callback)
}

/**
 * PUT 请求扩展函数
 */
inline fun <reified T> INetworkClient.put(
    url: String,
    callback: NetworkCallback<T>,
    noinline block: RequestBuilder.() -> Unit = {}
) {
    val request = RequestBuilder.create()
        .url(url)
        .method(HttpMethod.PUT)
        .apply(block)
        .build()

    execute(request, T::class.java, callback)
}

/**
 * DELETE 请求扩展函数
 */
inline fun <reified T> INetworkClient.delete(
    url: String,
    callback: NetworkCallback<T>,
    noinline block: RequestBuilder.() -> Unit = {}
) {
    val request = RequestBuilder.create()
        .url(url)
        .method(HttpMethod.DELETE)
        .apply(block)
        .build()

    execute(request, T::class.java, callback)
}

/**
 * 同步 GET 请求扩展函数
 */
inline fun <reified T> INetworkClient.getSync(
    url: String,
    noinline block: RequestBuilder.() -> Unit = {}
): Response<T> {
    val request = RequestBuilder.create()
        .url(url)
        .method(HttpMethod.GET)
        .apply(block)
        .build()

    return executeSync(request, T::class.java)
}

/**
 * 同步 POST 请求扩展函数
 */
inline fun <reified T> INetworkClient.postSync(
    url: String,
    noinline block: RequestBuilder.() -> Unit = {}
): Response<T> {
    val request = RequestBuilder.create()
        .url(url)
        .method(HttpMethod.POST)
        .apply(block)
        .build()

    return executeSync(request, T::class.java)
}
