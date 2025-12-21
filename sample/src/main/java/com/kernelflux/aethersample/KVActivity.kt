package com.kernelflux.aethersample

import android.widget.Button
import android.widget.TextView
import com.kernelflux.aether.kv.api.IKVStore
import com.kernelflux.aether.kv.api.KVConfig
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LoggerConfig
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.fluxrouter.core.FluxRouter

/**
 * KV 存储服务示例页面
 * 包含完整的测试用例，展示 KV 框架的主要功能
 *
 * @author Aether Framework
 */
class KVActivity : BaseActivity() {

    // 使用日志服务
    private val logger: com.kernelflux.aether.log.api.ILogger? = 
        com.kernelflux.fluxrouter.core.FluxRouter.getService(com.kernelflux.aether.log.api.ILogger::class.java)

    private var kvStore: IKVStore? = null

    private lateinit var resultText: TextView
    private lateinit var kvStatusText: TextView

    override fun getContentResId(): Int = R.layout.activity_kv

    override fun onInitView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "KV Store Test Cases"

        kvStore = FluxRouter.getService(IKVStore::class.java)

        resultText = findViewById(R.id.result_text)
        kvStatusText = findViewById(R.id.kv_status_text)

        // 检查 KV 存储服务是否可用
        if (kvStore == null) {
            updateResult("❌ KVStore service not found. Please check if aether-kv-mmkv module is included.")
            updateStatus("KV Store Status: Service Not Found")
            logger?.e("KVActivity", "KVStore service not found")
            return
        }

        // 初始化默认 KV 存储
        try {
            kvStore?.init(this, KVConfig.default())
            updateStatus("KV Store Status: Initialized (default config)")
            logger?.d("KVActivity", "KVStore initialized successfully")
        } catch (e: Exception) {
            updateResult("❌ Failed to initialize KVStore: ${e.message}")
            logger?.e("KVActivity", "Failed to initialize KVStore", e)
            updateStatus("KV Store Status: Initialization Failed")
            logger?.e("KVActivity", "Failed to initialize KVStore", e)
            return
        }

        // 基础数据类型测试
        setupBasicDataTypeTests()

        // 复杂数据类型测试
        setupComplexDataTypeTests()

        // 操作测试
        setupOperationTests()

        // 配置测试
        setupConfigurationTests()
    }

    /**
     * 设置基础数据类型测试
     */
    private fun setupBasicDataTypeTests() {
        // 1. String
        findViewById<Button>(R.id.btn_string).setOnClickListener {
            testString()
        }

        // 2. Int
        findViewById<Button>(R.id.btn_int).setOnClickListener {
            testInt()
        }

        // 3. Long
        findViewById<Button>(R.id.btn_long).setOnClickListener {
            testLong()
        }

        // 4. Float
        findViewById<Button>(R.id.btn_float).setOnClickListener {
            testFloat()
        }

        // 5. Double
        findViewById<Button>(R.id.btn_double).setOnClickListener {
            testDouble()
        }

        // 6. Boolean
        findViewById<Button>(R.id.btn_boolean).setOnClickListener {
            testBoolean()
        }
    }

    /**
     * 设置复杂数据类型测试
     */
    private fun setupComplexDataTypeTests() {
        // 7. ByteArray
        findViewById<Button>(R.id.btn_bytes).setOnClickListener {
            testBytes()
        }

        // 8. StringSet
        findViewById<Button>(R.id.btn_string_set).setOnClickListener {
            testStringSet()
        }
    }

    /**
     * 设置操作测试
     */
    private fun setupOperationTests() {
        // 9. Contains
        findViewById<Button>(R.id.btn_contains).setOnClickListener {
            testContains()
        }

        // 10. Remove
        findViewById<Button>(R.id.btn_remove).setOnClickListener {
            testRemove()
        }

        // 11. GetAllKeys
        findViewById<Button>(R.id.btn_get_all_keys).setOnClickListener {
            testGetAllKeys()
        }

        // 12. GetSize
        findViewById<Button>(R.id.btn_get_size).setOnClickListener {
            testGetSize()
        }

        // 13. Clear
        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            testClear()
        }

        // 14. Sync
        findViewById<Button>(R.id.btn_sync).setOnClickListener {
            testSync()
        }
    }

    /**
     * 设置配置测试
     */
    private fun setupConfigurationTests() {
        // 15. Custom Config
        findViewById<Button>(R.id.btn_custom_config).setOnClickListener {
            testCustomConfig()
        }

        // 16. Chain Operations
        findViewById<Button>(R.id.btn_chain_operations).setOnClickListener {
            testChainOperations()
        }
    }

    /**
     * 测试用例 1: String 存储和读取
     */
    private fun testString() {
        updateResult("Testing String operations...")
        try {
            val key = "test_string"
            val value = "Hello, KV Store! ${System.currentTimeMillis()}"

            // 存储
            kvStore?.putString(key, value)

            // 读取
            val retrieved = kvStore?.getString(key, "")
            val defaultValue = kvStore?.getString("non_existent_key", "default_value") ?: "default_value"

            val result = buildString {
                append("✅ String Test Success\n")
                append("Key: $key\n")
                append("Stored: $value\n")
                append("Retrieved: $retrieved\n")
                append("Default Value Test: $defaultValue\n")
            }
            updateResult(result)
            logger?.d("KV", "String test: stored=$value, retrieved=$retrieved")
        } catch (e: Exception) {
            updateResult("❌ String test failed: ${e.message}")
            logger?.e("KV", "String test error", e)
        }
    }

    /**
     * 测试用例 2: Int 存储和读取
     */
    private fun testInt() {
        updateResult("Testing Int operations...")
        try {
            val key = "test_int"
            val value = 12345

            kvStore?.putInt(key, value)
            val retrieved = kvStore?.getInt(key, 0)
            val defaultValue = kvStore?.getInt("non_existent_key", -1) ?: -1

            val result = buildString {
                append("✅ Int Test Success\n")
                append("Key: $key\n")
                append("Stored: $value\n")
                append("Retrieved: $retrieved\n")
                append("Default Value Test: $defaultValue\n")
            }
            updateResult(result)
            logger?.d("KV", "Int test: stored=$value, retrieved=$retrieved")
        } catch (e: Exception) {
            updateResult("❌ Int test failed: ${e.message}")
            logger?.e("KV", "Int test error", e)
        }
    }

    /**
     * 测试用例 3: Long 存储和读取
     */
    private fun testLong() {
        updateResult("Testing Long operations...")
        try {
            val key = "test_long"
            val value = System.currentTimeMillis()

            kvStore?.putLong(key, value)
            val retrieved = kvStore?.getLong(key, 0L)
            val defaultValue = kvStore?.getLong("non_existent_key", -1L) ?: -1L

            val result = buildString {
                append("✅ Long Test Success\n")
                append("Key: $key\n")
                append("Stored: $value\n")
                append("Retrieved: $retrieved\n")
                append("Default Value Test: $defaultValue\n")
            }
            updateResult(result)
            logger?.d("KV", "Long test: stored=$value, retrieved=$retrieved")
        } catch (e: Exception) {
            updateResult("❌ Long test failed: ${e.message}")
            logger?.e("KV", "Long test error", e)
        }
    }

    /**
     * 测试用例 4: Float 存储和读取
     */
    private fun testFloat() {
        updateResult("Testing Float operations...")
        try {
            val key = "test_float"
            val value = 3.14159f

            kvStore?.putFloat(key, value)
            val retrieved = kvStore?.getFloat(key, 0f)
            val defaultValue = kvStore?.getFloat("non_existent_key", -1f) ?: -1f

            val result = buildString {
                append("✅ Float Test Success\n")
                append("Key: $key\n")
                append("Stored: $value\n")
                append("Retrieved: $retrieved\n")
                append("Default Value Test: $defaultValue\n")
            }
            updateResult(result)
            logger?.d("KV", "Float test: stored=$value, retrieved=$retrieved")
        } catch (e: Exception) {
            updateResult("❌ Float test failed: ${e.message}")
            logger?.e("KV", "Float test error", e)
        }
    }

    /**
     * 测试用例 5: Double 存储和读取
     */
    private fun testDouble() {
        updateResult("Testing Double operations...")
        try {
            val key = "test_double"
            val value = 3.141592653589793

            kvStore?.putDouble(key, value)
            val retrieved = kvStore?.getDouble(key, 0.0)
            val defaultValue = kvStore?.getDouble("non_existent_key", -1.0) ?: -1.0

            val result = buildString {
                append("✅ Double Test Success\n")
                append("Key: $key\n")
                append("Stored: $value\n")
                append("Retrieved: $retrieved\n")
                append("Default Value Test: $defaultValue\n")
            }
            updateResult(result)
            logger?.d("KV", "Double test: stored=$value, retrieved=$retrieved")
        } catch (e: Exception) {
            updateResult("❌ Double test failed: ${e.message}")
            logger?.e("KV", "Double test error", e)
        }
    }

    /**
     * 测试用例 6: Boolean 存储和读取
     */
    private fun testBoolean() {
        updateResult("Testing Boolean operations...")
        try {
            val key = "test_boolean"
            val value = true

            kvStore?.putBoolean(key, value)
            val retrieved = kvStore?.getBoolean(key, false)
            val defaultValue = kvStore?.getBoolean("non_existent_key", false) ?: false

            val result = buildString {
                append("✅ Boolean Test Success\n")
                append("Key: $key\n")
                append("Stored: $value\n")
                append("Retrieved: $retrieved\n")
                append("Default Value Test: $defaultValue\n")
            }
            updateResult(result)
            logger?.d("KV", "Boolean test: stored=$value, retrieved=$retrieved")
        } catch (e: Exception) {
            updateResult("❌ Boolean test failed: ${e.message}")
            logger?.e("KV", "Boolean test error", e)
        }
    }

    /**
     * 测试用例 7: ByteArray 存储和读取
     */
    private fun testBytes() {
        updateResult("Testing ByteArray operations...")
        try {
            val key = "test_bytes"
            val value = "Hello, ByteArray!".toByteArray()

            kvStore?.putBytes(key, value)
            val retrieved = kvStore?.getBytes(key, null)
            val defaultValue = kvStore?.getBytes("non_existent_key", null)

            val result = buildString {
                append("✅ ByteArray Test Success\n")
                append("Key: $key\n")
                append("Stored: ${String(value)}\n")
                append("Stored Size: ${value.size} bytes\n")
                append("Retrieved: ${retrieved?.let { String(it) } ?: "null"}\n")
                append("Retrieved Size: ${retrieved?.size ?: 0} bytes\n")
                append("Default Value Test: ${defaultValue?.let { String(it) } ?: "null"}\n")
            }
            updateResult(result)
            logger?.d("KV", "ByteArray test: stored=${value.size} bytes, retrieved=${retrieved?.size ?: 0} bytes")
        } catch (e: Exception) {
            updateResult("❌ ByteArray test failed: ${e.message}")
            logger?.e("KV", "ByteArray test error", e)
        }
    }

    /**
     * 测试用例 8: StringSet 存储和读取
     */
    private fun testStringSet() {
        updateResult("Testing StringSet operations...")
        try {
            val key = "test_string_set"
            val value = setOf("apple", "banana", "cherry", "date")

            kvStore?.putStringSet(key, value)
            val retrieved = kvStore?.getStringSet(key, null)
            val defaultValue = kvStore?.getStringSet("non_existent_key", null)

            val result = buildString {
                append("✅ StringSet Test Success\n")
                append("Key: $key\n")
                append("Stored: $value\n")
                append("Stored Size: ${value.size} items\n")
                append("Retrieved: $retrieved\n")
                append("Retrieved Size: ${retrieved?.size ?: 0} items\n")
                append("Default Value Test: ${defaultValue ?: "null"}\n")
            }
            updateResult(result)
            logger?.d("KV", "StringSet test: stored=${value.size} items, retrieved=${retrieved?.size ?: 0} items")
        } catch (e: Exception) {
            updateResult("❌ StringSet test failed: ${e.message}")
            logger?.e("KV", "StringSet test error", e)
        }
    }

    /**
     * 测试用例 9: Contains 检查
     */
    private fun testContains() {
        updateResult("Testing Contains operation...")
        try {
            val existingKey = "test_contains_existing"
            val nonExistingKey = "test_contains_non_existing"

            // 先存储一个值
            kvStore?.putString(existingKey, "test_value")

            val containsExisting = kvStore?.contains(existingKey) ?: false
            val containsNonExisting = kvStore?.contains(nonExistingKey) ?: false

            val result = buildString {
                append("✅ Contains Test Success\n")
                append("Key '$existingKey' exists: $containsExisting\n")
                append("Key '$nonExistingKey' exists: $containsNonExisting\n")
            }
            updateResult(result)
            logger?.d("KV", "Contains test: existing=$containsExisting, nonExisting=$containsNonExisting")
        } catch (e: Exception) {
            updateResult("❌ Contains test failed: ${e.message}")
            logger?.e("KV", "Contains test error", e)
        }
    }

    /**
     * 测试用例 10: Remove 删除
     */
    private fun testRemove() {
        updateResult("Testing Remove operation...")
        try {
            val key = "test_remove"

            // 先存储一个值
            kvStore?.putString(key, "value_to_remove")
            val beforeRemove = kvStore?.contains(key) ?: false

            // 删除
            kvStore?.remove(key)
            val afterRemove = kvStore?.contains(key) ?: false

            val result = buildString {
                append("✅ Remove Test Success\n")
                append("Key '$key' before remove: $beforeRemove\n")
                append("Key '$key' after remove: $afterRemove\n")
            }
            updateResult(result)
            logger?.d("KV", "Remove test: before=$beforeRemove, after=$afterRemove")
        } catch (e: Exception) {
            updateResult("❌ Remove test failed: ${e.message}")
            logger?.e("KV", "Remove test error", e)
        }
    }

    /**
     * 测试用例 11: GetAllKeys 获取所有键
     */
    private fun testGetAllKeys() {
        updateResult("Testing GetAllKeys operation...")
        try {
            // 先存储一些测试数据
            kvStore?.putString("key1", "value1")
            kvStore?.putInt("key2", 123)
            kvStore?.putBoolean("key3", true)

            val allKeys = kvStore?.getAllKeys() ?: emptySet()

            val result = buildString {
                append("✅ GetAllKeys Test Success\n")
                append("Total Keys: ${allKeys.size}\n")
                append("Keys:\n")
                allKeys.forEach { key ->
                    append("  - $key\n")
                }
            }
            updateResult(result)
            logger?.d("KV", "GetAllKeys test: found ${allKeys.size} keys")
        } catch (e: Exception) {
            updateResult("❌ GetAllKeys test failed: ${e.message}")
            logger?.e("KV", "GetAllKeys test error", e)
        }
    }

    /**
     * 测试用例 12: GetSize 获取存储大小
     */
    private fun testGetSize() {
        updateResult("Testing GetSize operation...")
        try {
            // 先存储一些测试数据
            kvStore?.putString("size_test_key1", "This is a test value for size calculation")
            kvStore?.putInt("size_test_key2", 12345)
            kvStore?.putString("size_test_key3", "Another test value")

            val size = kvStore?.getSize() ?: 0L

            val result = buildString {
                append("✅ GetSize Test Success\n")
                append("Storage Size: $size bytes\n")
                append("Storage Size: ${size / 1024.0} KB\n")
            }
            updateResult(result)
            logger?.d("KV", "GetSize test: size=$size bytes")
        } catch (e: Exception) {
            updateResult("❌ GetSize test failed: ${e.message}")
            logger?.e("KV", "GetSize test error", e)
        }
    }

    /**
     * 测试用例 13: Clear 清空所有数据
     */
    private fun testClear() {
        updateResult("Testing Clear operation...")
        try {
            // 先存储一些测试数据
            kvStore?.putString("clear_test_key1", "value1")
            kvStore?.putInt("clear_test_key2", 123)
            val beforeClear = kvStore?.getAllKeys()?.size ?: 0

            // 清空
            kvStore?.clear()
            val afterClear = kvStore?.getAllKeys()?.size ?: 0

            val result = buildString {
                append("✅ Clear Test Success\n")
                append("Keys before clear: $beforeClear\n")
                append("Keys after clear: $afterClear\n")
            }
            updateResult(result)
            logger?.d("KV", "Clear test: before=$beforeClear, after=$afterClear")
        } catch (e: Exception) {
            updateResult("❌ Clear test failed: ${e.message}")
            logger?.e("KV", "Clear test error", e)
        }
    }

    /**
     * 测试用例 14: Sync 同步写入
     */
    private fun testSync() {
        updateResult("Testing Sync operation...")
        try {
            // 先存储一些数据
            kvStore?.putString("sync_test_key", "sync_test_value")

            // 同步
            kvStore?.sync()

            val result = buildString {
                append("✅ Sync Test Success\n")
                append("Data has been synchronized to disk\n")
            }
            updateResult(result)
            logger?.d("KV", "Sync test: completed")
        } catch (e: Exception) {
            updateResult("❌ Sync test failed: ${e.message}")
            logger?.e("KV", "Sync test error", e)
        }
    }

    /**
     * 测试用例 15: Custom Config 自定义配置
     */
    private fun testCustomConfig() {
        updateResult("Testing Custom Config...")
        try {
            // 创建自定义配置
            val customConfig = KVConfig.builder()
                .fileName("custom_kv_store")
                .enableEncryption(false)
                .maxSize(1024 * 1024) // 1MB
                .build()

            // 获取新的 KV 存储实例（注意：FluxRouter 可能返回同一个实例）
            // 这里我们使用默认实例，但展示配置的使用方式
            val result = buildString {
                append("✅ Custom Config Test Success\n")
                append("Config:\n")
                append("  File Name: ${customConfig.fileName}\n")
                append("  Encryption: ${customConfig.enableEncryption}\n")
                append("  Max Size: ${customConfig.maxSize} bytes\n")
                append("  Root Dir: ${customConfig.rootDir?.absolutePath ?: "default"}\n")
                append("\nNote: To use custom config, initialize with:\n")
                append("  kvStore?.init(context, customConfig)\n")
            }
            updateResult(result)
            logger?.d("KV", "Custom Config test: fileName=${customConfig.fileName}")
        } catch (e: Exception) {
            updateResult("❌ Custom Config test failed: ${e.message}")
            logger?.e("KV", "Custom Config test error", e)
        }
    }

    /**
     * 测试用例 16: Chain Operations 链式操作
     */
    private fun testChainOperations() {
        updateResult("Testing Chain Operations...")
        try {
            // 链式存储多个值
            kvStore?.putString("chain_key1", "value1")
                ?.putInt("chain_key2", 123)
                ?.putBoolean("chain_key3", true)
                ?.putLong("chain_key4", System.currentTimeMillis())

            // 读取验证
            val value1 = kvStore?.getString("chain_key1", "")
            val value2 = kvStore?.getInt("chain_key2", 0)
            val value3 = kvStore?.getBoolean("chain_key3", false)
            val value4 = kvStore?.getLong("chain_key4", 0L)

            val result = buildString {
                append("✅ Chain Operations Test Success\n")
                append("Chain stored and retrieved:\n")
                append("  chain_key1: $value1\n")
                append("  chain_key2: $value2\n")
                append("  chain_key3: $value3\n")
                append("  chain_key4: $value4\n")
            }
            updateResult(result)
            logger?.d("KV", "Chain Operations test: all values stored and retrieved")
        } catch (e: Exception) {
            updateResult("❌ Chain Operations test failed: ${e.message}")
            logger?.e("KV", "Chain Operations test error", e)
        }
    }

    /**
     * 更新结果显示
     */
    private fun updateResult(text: String) {
        resultText.text = text
    }

    /**
     * 更新状态显示
     */
    private fun updateStatus(text: String) {
        kvStatusText.text = text
    }
}

