package com.kernelflux.aethersample

import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.network.api.*
import com.kernelflux.aether.network.api.DefaultRetryStrategy
import com.kernelflux.aether.network.impl.okhttp.converter.DataConverterFactory
import com.kernelflux.aether.network.impl.okhttp.crypto.AesCrypto
import com.kernelflux.aether.network.impl.okhttp.crypto.CryptoFactory
import com.kernelflux.aether.network.impl.okhttp.crypto.RsaCrypto
import com.kernelflux.fluxrouter.core.FluxRouter
import kotlinx.coroutines.launch

/**
 * 网络请求服务示例页面
 * 包含完整的测试用例，展示网络框架的主要功能
 *
 * @author Aether Framework
 */
class NetworkActivity : BaseActivity() {

    private var networkClient: INetworkClient? = null
    private var logger: ILogger? = null
    private var requestTag: Any? = null

    private lateinit var resultText: TextView
    private lateinit var networkStateText: TextView

    override fun getContentResId(): Int = R.layout.activity_network

    override fun onInitView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Network Test Cases"

        networkClient = FluxRouter.getService(INetworkClient::class.java)
        logger = FluxRouter.getService(ILogger::class.java)

        resultText = findViewById(R.id.result_text)
        networkStateText = findViewById(R.id.network_state_text)

        // 检查网络客户端是否已初始化
        if (networkClient == null) {
            updateResult("❌ NetworkClient service not found. Please check if aether-network-okhttp module is included.")
            logger?.e("NetworkActivity_tag", "NetworkClient service not found")
            return
        }
        
        android.util.Log.d("NetworkActivity", "NetworkClient instance: ${networkClient.hashCode()}")

        // 设置网络状态监听
        setupNetworkStateMonitoring()

        // 基础请求测试
        setupBasicRequestTests()

        // 高级功能测试
        setupAdvancedFeatureTests()
        
        // 数据格式和加密测试
        setupDataFormatAndCryptoTests()
    }

    /**
     * 设置网络状态监听（Flow）
     */
    private fun setupNetworkStateMonitoring() {
        networkClient?.let { client ->
            // 使用 Flow 监听网络状态
            lifecycleScope.launch {
                client.getNetworkStateFlow().collect { state ->
                    runOnUiThread {
                        networkStateText.text = "Network State: $state"
                        logger?.d("NetworkActivity_tag", "Network state changed: $state")
                    }
                }
            }

            // 添加网络状态监听器
            client.addNetworkStateListener(object : NetworkStateListener {
                override fun onNetworkStateChanged(oldState: NetworkState, newState: NetworkState) {
                    logger?.d("NetworkActivity_tag", "Network state: $oldState -> $newState")
                }
            })
        }
    }

    /**
     * 设置基础请求测试
     */
    private fun setupBasicRequestTests() {
        // 1. GET 请求
        findViewById<Button>(R.id.btn_get).setOnClickListener {
            testGetRequest()
        }

        // 2. POST JSON 请求
        findViewById<Button>(R.id.btn_post_json).setOnClickListener {
            testPostJsonRequest()
        }

        // 3. POST Form 请求
        findViewById<Button>(R.id.btn_post_form).setOnClickListener {
            testPostFormRequest()
        }

        // 4. PUT 请求
        findViewById<Button>(R.id.btn_put).setOnClickListener {
            testPutRequest()
        }

        // 5. DELETE 请求
        findViewById<Button>(R.id.btn_delete).setOnClickListener {
            testDeleteRequest()
        }
    }

    /**
     * 设置高级功能测试
     */
    private fun setupAdvancedFeatureTests() {
        // 6. 带请求头的请求
        findViewById<Button>(R.id.btn_with_headers).setOnClickListener {
            testRequestWithHeaders()
        }

        // 7. 带查询参数的请求
        findViewById<Button>(R.id.btn_with_params).setOnClickListener {
            testRequestWithParams()
        }

        // 8. 缓存测试
        findViewById<Button>(R.id.btn_cache).setOnClickListener {
            testCache()
        }

        // 9. 重试测试
        findViewById<Button>(R.id.btn_retry).setOnClickListener {
            testRetry()
        }

        // 10. 同步请求
        findViewById<Button>(R.id.btn_sync).setOnClickListener {
            testSyncRequest()
        }

        // 11. 取消请求
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            testCancelRequest()
        }

        // 12. DSL 构建器
        findViewById<Button>(R.id.btn_dsl).setOnClickListener {
            testDslBuilder()
        }
    }

    /**
     * 设置数据格式和加密测试
     */
    private fun setupDataFormatAndCryptoTests() {
        // 13. JSON 转换器
        findViewById<Button>(R.id.btn_json_converter).setOnClickListener {
            testJsonConverter()
        }

        // 14. Protobuf 转换器
        findViewById<Button>(R.id.btn_protobuf_converter).setOnClickListener {
            testProtobufConverter()
        }

        // 15. 格式切换
        findViewById<Button>(R.id.btn_format_switch).setOnClickListener {
            testFormatSwitch()
        }

        // 16. AES 加密
        findViewById<Button>(R.id.btn_aes_encryption).setOnClickListener {
            testAesEncryption()
        }

        // 17. RSA 加密
        findViewById<Button>(R.id.btn_rsa_encryption).setOnClickListener {
            testRsaEncryption()
        }
    }

    /**
     * 测试用例 1: GET 请求
     */
    private fun testGetRequest() {
        updateResult("Testing GET request...")
        networkClient?.get<String>(
            url = "https://api.github.com/users/octocat",
            callback = createCallback("GET Request")
        )
    }

    /**
     * 测试用例 2: POST JSON 请求
     */
    private fun testPostJsonRequest() {
        updateResult("Testing POST JSON request...")
        val requestBody = mapOf(
            "name" to "test",
            "value" to 123,
            "active" to true
        )
        networkClient?.post<String>(
            url = "https://httpbin.org/post",
            callback = createCallback("POST JSON Request")
        ) {
            jsonBody(requestBody)
        }
    }

    /**
     * 测试用例 3: POST Form 请求
     */
    private fun testPostFormRequest() {
        updateResult("Testing POST Form request...")
        val formData = mapOf(
            "username" to "testuser",
            "password" to "testpass",
            "email" to "test@example.com"
        )
        networkClient?.post<String>(
            url = "https://httpbin.org/post",
            callback = createCallback("POST Form Request")
        ) {
            formBody(formData)
        }
    }

    /**
     * 测试用例 4: PUT 请求
     */
    private fun testPutRequest() {
        updateResult("Testing PUT request...")
        val updateData = mapOf(
            "id" to 1,
            "name" to "updated name",
            "status" to "active"
        )
        networkClient?.put<String>(
            url = "https://httpbin.org/put",
            callback = createCallback("PUT Request")
        ) {
            jsonBody(updateData)
        }
    }

    /**
     * 测试用例 5: DELETE 请求
     */
    private fun testDeleteRequest() {
        updateResult("Testing DELETE request...")
        networkClient?.delete<String>(
            url = "https://httpbin.org/delete",
            callback = createCallback("DELETE Request")
        )
    }

    /**
     * 测试用例 6: 带请求头的请求
     */
    private fun testRequestWithHeaders() {
        updateResult("Testing request with headers...")
        networkClient?.get<String>(
            url = "https://httpbin.org/headers",
            callback = createCallback("Request with Headers")
        ) {
            header("X-Custom-Header", "custom-value")
            header("X-Request-ID", "12345")
            header("Authorization", "Bearer token123")
            headers(mapOf(
                "User-Agent" to "Aether-Client/1.0",
                "Accept" to "application/json"
            ))
        }
    }

    /**
     * 测试用例 7: 带查询参数的请求
     */
    private fun testRequestWithParams() {
        updateResult("Testing request with params...")
        networkClient?.get<String>(
            url = "https://httpbin.org/get",
            callback = createCallback("Request with Params")
        ) {
            param("page", 1)
            param("limit", 20)
            param("sort", "desc")
            params(mapOf(
                "filter" to "active",
                "category" to "test"
            ))
        }
    }

    /**
     * 测试用例 8: 缓存测试
     */
    private fun testCache() {
        updateResult("Testing cache...")
        networkClient?.get<String>(
            url = "https://api.github.com/users/octocat",
            callback = createCallback("Cache Test")
        ) {
            cacheStrategy(CacheStrategy.CACHE_FIRST)
        }
    }

    /**
     * 测试用例 9: 重试测试
     */
    private fun testRetry() {
        updateResult("Testing retry...")
        networkClient?.get<String>(
            url = "https://httpbin.org/status/500", // 模拟服务器错误
            callback = createCallback("Retry Test")
        ) {
            retryStrategy(DefaultRetryStrategy(
                maxRetries = 3,
                retryOnNetworkError = true,
                retryOnHttpError = true,
                baseDelayMs = 1000L
            ))
        }
    }

    /**
     * 测试用例 10: 同步请求
     */
    private fun testSyncRequest() {
        updateResult("Testing sync request...")
        Thread {
            try {
                val response = networkClient?.getSync<String>(
                    url = "https://api.github.com/users/octocat"
                )
                runOnUiThread {
                    if (response?.isSuccessful == true) {
                        updateResult("Sync Request Success:\n${response.data}")
                    } else {
                        updateResult("Sync Request Failed: ${response?.message}")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    updateResult("Sync Request Error: ${e.message}")
                }
            }
        }.start()
    }

    /**
     * 测试用例 11: 取消请求
     */
    private fun testCancelRequest() {
        updateResult("Testing cancel request...")
        requestTag = "cancel-test-${System.currentTimeMillis()}"
        
        networkClient?.get<String>(
            url = "https://httpbin.org/delay/5", // 延迟 5 秒的请求
            callback = createCallback("Cancel Test")
        ) {
            tag(requestTag!!)
        }

        // 2 秒后取消请求
        resultText.postDelayed({
            networkClient?.cancel(requestTag!!)
            updateResult("Request cancelled after 2 seconds")
        }, 2000)
    }

    /**
     * 测试用例 12: DSL 构建器
     */
    private fun testDslBuilder() {
        updateResult("Testing DSL builder...")
        val request = RequestBuilder.create()
            .url("https://httpbin.org/post")
            .method(HttpMethod.POST)
            .header("Content-Type", "application/json")
            .header("X-Request-ID", "dsl-test-123")
            .param("source", "dsl")
            .jsonBody(mapOf(
                "message" to "DSL Builder Test",
                "timestamp" to System.currentTimeMillis()
            ))
            .cacheStrategy(CacheStrategy.NO_CACHE)
            .retryStrategy(DefaultRetryStrategy(
                maxRetries = 2,
                baseDelayMs = 500L
            ))
            .tag("dsl-test")
            .build()

        networkClient?.execute(
            request = request,
            responseType = String::class.java,
            callback = createCallback("DSL Builder Test")
        )
    }

    /**
     * 创建回调对象
     */
    private fun createCallback(testName: String): NetworkCallback<String> {
        return object : NetworkCallback<String> {
            override fun onSuccess(response: Response<String>) {
                runOnUiThread {
                    val result = buildString {
                        append("✅ $testName Success\n")
                        append("Status: ${response.code}\n")
                        append("Message: ${response.message}\n")
                        append("Headers: ${response.headers}\n")
                        append("From Cache: ${response.isFromCache}\n")
                        append("Data: ${response.data?.take(500)}\n")
                        if (response.data != null && response.data!!.length > 500) {
                            append("... (truncated)")
                        }
                    }
                    updateResult(result)
                }
                logger?.d("NetworkActivity_tag", "$testName success: ${response.data}")
            }

            override fun onError(exception: NetworkException) {
                runOnUiThread {
                    val result = buildString {
                        append("❌ $testName Failed\n")
                        append("Error: ${exception.message}\n")
                        when (exception) {
                            is NetworkException.HttpException -> {
                                append("HTTP Status: ${exception.code}\n")
                                append("Response: ${exception.response?.data?.toString()?.take(200)}")
                            }
                            is NetworkException.ConnectionException -> {
                                append("Connection Error: ${exception.cause?.message}")
                            }
                            is NetworkException.TimeoutException -> {
                                append("Timeout: ${exception.message}")
                            }
                            is NetworkException.ParseException -> {
                                append("Parse Error: ${exception.message}")
                            }
                            is NetworkException.CancelledException -> {
                                append("Cancelled: ${exception.message}")
                            }
                            else -> {
                                append("Unknown Error: ${exception.message}")
                            }
                        }
                    }
                    updateResult(result)
                }
                logger?.e("NetworkActivity_tag", "$testName error", exception)
            }
        }
    }

    /**
     * 测试用例 13: JSON 转换器
     */
    private fun testJsonConverter() {
        updateResult("Testing JSON converter...")
        val jsonConverter = DataConverterFactory.createJson()
        val testData = mapOf(
            "name" to "Test User",
            "age" to 25,
            "active" to true,
            "tags" to listOf("android", "kotlin", "network")
        )
        
        networkClient?.post<String>(
            url = "https://httpbin.org/post",
            callback = createCallback("JSON Converter Test")
        ) {
            jsonBody(testData)
            dataConverter(jsonConverter)
        }
    }

    /**
     * 测试用例 14: Protobuf 转换器
     */
    private fun testProtobufConverter() {
        updateResult("Testing Protobuf converter...")
        val protobufConverter = DataConverterFactory.createProtobuf()
        
        // 注意：这里使用模拟的 Protobuf 数据（字节数组）
        // 实际使用时，应该使用真实的 Protobuf Message 对象
        val protobufData = "Hello Protobuf!".toByteArray()
        
        networkClient?.post<String>(
            url = "https://httpbin.org/post",
            callback = createCallback("Protobuf Converter Test")
        ) {
            protobufBody(protobufData)
            dataConverter(protobufConverter)
        }
    }

    /**
     * 测试用例 15: 格式切换（JSON ↔ Protobuf）
     */
    private fun testFormatSwitch() {
        updateResult("Testing format switch...")
        
        val testData = mapOf(
            "message" to "Format Switch Test",
            "timestamp" to System.currentTimeMillis(),
            "format" to "JSON"
        )
        
        // 先使用 JSON 格式
        updateResult("Step 1: Sending with JSON converter...")
        val jsonConverter = DataConverterFactory.createJson()
        networkClient?.post<String>(
            url = "https://httpbin.org/post",
            callback = object : NetworkCallback<String> {
                override fun onSuccess(response: Response<String>) {
                    runOnUiThread {
                        updateResult("✅ JSON format success!\n\nStep 2: Sending with Protobuf converter...")
                        // 然后使用 Protobuf 格式
                        val protobufConverter = DataConverterFactory.createProtobuf()
                        val protobufData = "Protobuf Format Test".toByteArray()
                        networkClient?.post<String>(
                            url = "https://httpbin.org/post",
                            callback = createCallback("Format Switch (Protobuf)")
                        ) {
                            protobufBody(protobufData)
                            dataConverter(protobufConverter)
                        }
                    }
                }

                override fun onError(exception: NetworkException) {
                    runOnUiThread {
                        updateResult("❌ JSON format failed: ${exception.message}")
                    }
                }
            }
        ) {
            jsonBody(testData)
            dataConverter(jsonConverter)
        }
    }

    /**
     * 测试用例 16: AES 加密
     */
    private fun testAesEncryption() {
        updateResult("Testing AES encryption...")
        
        try {
            // 生成 AES 密钥（实际使用时应该安全存储）
            val aesKey = AesCrypto.generateKey(256)
            val aesCrypto = AesCrypto(aesKey)
            
            val sensitiveData = mapOf(
                "username" to "testuser",
                "password" to "secret123",
                "creditCard" to "1234-5678-9012-3456"
            )
            
            updateResult("Encrypting sensitive data with AES-256...")
            networkClient?.post<String>(
                url = "https://httpbin.org/post",
                callback = createCallback("AES Encryption Test")
            ) {
                jsonBody(sensitiveData)
                encryptor(aesCrypto)
                decryptor(aesCrypto)
            }
        } catch (e: Exception) {
            updateResult("❌ AES encryption test failed: ${e.message}")
            logger?.e("NetworkActivity_tag", "AES encryption test error", e)
        }
    }

    /**
     * 测试用例 17: RSA 加密
     */
    private fun testRsaEncryption() {
        updateResult("Testing RSA encryption...")
        
        try {
            // 生成 RSA 密钥对（实际使用时应该安全存储）
            val keyPair = RsaCrypto.generateKeyPair(2048)
            val rsaCrypto = RsaCrypto(keyPair.public, keyPair.private)
            
            val sensitiveData = mapOf(
                "apiKey" to "sk-1234567890abcdef",
                "secretToken" to "token-secret-value",
                "userId" to "user-12345"
            )
            
            updateResult("Encrypting sensitive data with RSA-2048...")
            networkClient?.post<String>(
                url = "https://httpbin.org/post",
                callback = createCallback("RSA Encryption Test")
            ) {
                jsonBody(sensitiveData)
                encryptor(rsaCrypto)
                decryptor(rsaCrypto)
            }
        } catch (e: Exception) {
            updateResult("❌ RSA encryption test failed: ${e.message}")
            logger?.e("NetworkActivity_tag", "RSA encryption test error", e)
        }
    }

    /**
     * 更新结果显示
     */
    private fun updateResult(text: String) {
        resultText.text = text
    }

    override fun onDestroy() {
        super.onDestroy()
        // 移除网络状态监听器
        networkClient?.removeNetworkStateListener(object : NetworkStateListener {
            override fun onNetworkStateChanged(oldState: NetworkState, newState: NetworkState) {
                // 已移除，不会回调
            }
        })
    }
}
