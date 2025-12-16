package com.kernelflux.aether.kv.mmkv

import android.content.Context
import com.kernelflux.aether.kv.api.IKVStore
import com.kernelflux.aether.kv.api.KVConfig
import com.kernelflux.fluxrouter.annotation.FluxService
import com.tencent.mmkv.MMKV
import java.io.File

/**
 * 检查是否为 Debug 构建
 * 注意：Android 库模块会自动生成 BuildConfig 类
 */
private fun isDebugBuild(): Boolean {
    return try {
        // 使用 BuildConfig（Android 库模块会自动生成）
        // 注意：BuildConfig 在编译时生成，需要确保模块已正确配置
        val buildConfigClass = Class.forName("com.kernelflux.aether.kv.mmkv.BuildConfig")
        val debugField = buildConfigClass.getField("DEBUG")
        debugField.getBoolean(null)
    } catch (e: Exception) {
        // 如果无法获取，默认返回 false（生产环境安全）
        // 这样可以确保在生产环境中不会因为异常检查而崩溃
        false
    }
}

/**
 * 安全地抛出异常（仅在 Debug 构建中抛出）
 * 
 * 开发环境：抛出异常，便于调试
 * 生产环境：记录错误日志，但不崩溃，返回默认值或空操作
 */
private fun throwIfDebug(exception: RuntimeException) {
    if (isDebugBuild()) {
        throw exception
    } else {
        // 生产环境：记录错误但不崩溃
        android.util.Log.e("MMKVStore", "Error occurred (suppressed in release): ${exception.message}", exception)
    }
}

/**
 * MMKV 实现的 Key-Value 存储服务
 * 
 * 基于腾讯开源的 MMKV，提供高性能的键值对存储
 * 
 * @author Aether Framework
 */
@FluxService(interfaceClass = IKVStore::class)
class MMKVStore : IKVStore {
    
    private var mmkv: MMKV? = null
    private var config: KVConfig? = null
    
    /**
     * 初始化存储
     * @param context Android Context
     * @param config 配置
     */
    override fun init(context: Any, config: KVConfig) {
        val androidContext = context as? Context
        if (androidContext == null) {
            val exception = IllegalArgumentException("Context must be an Android Context")
            throwIfDebug(exception)
            android.util.Log.e("MMKVStore", "Invalid context type: ${context.javaClass.name}")
            return
        }
        
        this.config = config
        
        try {
            // 初始化 MMKV
            val rootDir = config.rootDir ?: run {
                val appContext = androidContext.applicationContext ?: androidContext
                File(appContext.filesDir, "mmkv")
            }
            rootDir.mkdirs()
            
            // 初始化 MMKV（推荐使用 Context 参数的方法）
            // 如果 MMKV 已经初始化过，这里不会重复初始化
            val initRootDir = MMKV.initialize(androidContext, rootDir.absolutePath)
            android.util.Log.d("MMKVStore", "MMKV initialized at: $initRootDir")
            
            // 创建或获取 MMKV 实例
            mmkv = if (config.enableEncryption && config.encryptionKey != null) {
                // 加密模式
                MMKV.mmkvWithID(config.fileName, MMKV.MULTI_PROCESS_MODE, config.encryptionKey)
            } else {
                // 普通模式
                MMKV.mmkvWithID(config.fileName, MMKV.MULTI_PROCESS_MODE)
            }
            
            // 注意：MMKV 的存储大小限制需要在初始化时设置
            // 如果设置了最大大小，可以通过 MMKV 的配置来限制
            // 但 MMKV 本身没有直接的 setValueSizeLimit 方法
            // 这里可以通过定期清理或监控来实现大小限制
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to initialize MMKV", e)
            val exception = RuntimeException("Failed to initialize MMKV", e)
            throwIfDebug(exception)
            // 生产环境：初始化失败但不崩溃，后续操作会返回默认值
        }
    }
    
    private fun getMMKV(): MMKV? {
        if (mmkv == null) {
            val exception = IllegalStateException("MMKVStore not initialized. Call init() first.")
            throwIfDebug(exception)
            android.util.Log.w("MMKVStore", "MMKVStore not initialized. Operations will be ignored.")
        }
        return mmkv
    }
    
    override fun putString(key: String, value: String): IKVStore {
        try {
            getMMKV()?.encode(key, value)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to putString: $key", e)
            throwIfDebug(RuntimeException("Failed to putString: $key", e))
        }
        return this
    }
    
    override fun getString(key: String, defaultValue: String): String {
        return try {
            getMMKV()?.decodeString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getString: $key", e)
            throwIfDebug(RuntimeException("Failed to getString: $key", e))
            defaultValue
        }
    }
    
    override fun putInt(key: String, value: Int): IKVStore {
        try {
            getMMKV()?.encode(key, value)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to putInt: $key", e)
            throwIfDebug(RuntimeException("Failed to putInt: $key", e))
        }
        return this
    }
    
    override fun getInt(key: String, defaultValue: Int): Int {
        return try {
            getMMKV()?.decodeInt(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getInt: $key", e)
            throwIfDebug(RuntimeException("Failed to getInt: $key", e))
            defaultValue
        }
    }
    
    override fun putLong(key: String, value: Long): IKVStore {
        try {
            getMMKV()?.encode(key, value)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to putLong: $key", e)
            throwIfDebug(RuntimeException("Failed to putLong: $key", e))
        }
        return this
    }
    
    override fun getLong(key: String, defaultValue: Long): Long {
        return try {
            getMMKV()?.decodeLong(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getLong: $key", e)
            throwIfDebug(RuntimeException("Failed to getLong: $key", e))
            defaultValue
        }
    }
    
    override fun putFloat(key: String, value: Float): IKVStore {
        try {
            getMMKV()?.encode(key, value)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to putFloat: $key", e)
            throwIfDebug(RuntimeException("Failed to putFloat: $key", e))
        }
        return this
    }
    
    override fun getFloat(key: String, defaultValue: Float): Float {
        return try {
            getMMKV()?.decodeFloat(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getFloat: $key", e)
            throwIfDebug(RuntimeException("Failed to getFloat: $key", e))
            defaultValue
        }
    }
    
    override fun putDouble(key: String, value: Double): IKVStore {
        try {
            getMMKV()?.encode(key, value)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to putDouble: $key", e)
            throwIfDebug(RuntimeException("Failed to putDouble: $key", e))
        }
        return this
    }
    
    override fun getDouble(key: String, defaultValue: Double): Double {
        return try {
            getMMKV()?.decodeDouble(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getDouble: $key", e)
            throwIfDebug(RuntimeException("Failed to getDouble: $key", e))
            defaultValue
        }
    }
    
    override fun putBoolean(key: String, value: Boolean): IKVStore {
        try {
            getMMKV()?.encode(key, value)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to putBoolean: $key", e)
            throwIfDebug(RuntimeException("Failed to putBoolean: $key", e))
        }
        return this
    }
    
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            getMMKV()?.decodeBool(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getBoolean: $key", e)
            throwIfDebug(RuntimeException("Failed to getBoolean: $key", e))
            defaultValue
        }
    }
    
    override fun putBytes(key: String, value: ByteArray): IKVStore {
        try {
            getMMKV()?.encode(key, value)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to putBytes: $key", e)
            throwIfDebug(RuntimeException("Failed to putBytes: $key", e))
        }
        return this
    }
    
    override fun getBytes(key: String, defaultValue: ByteArray?): ByteArray? {
        return try {
            getMMKV()?.decodeBytes(key, defaultValue)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getBytes: $key", e)
            throwIfDebug(RuntimeException("Failed to getBytes: $key", e))
            defaultValue
        }
    }
    
    override fun putStringSet(key: String, value: Set<String>): IKVStore {
        try {
            // MMKV 的 encode 方法可以直接接受 Set<String>
            getMMKV()?.encode(key, value)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to putStringSet: $key", e)
            throwIfDebug(RuntimeException("Failed to putStringSet: $key", e))
        }
        return this
    }
    
    override fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? {
        return try {
            val mmkv = getMMKV() ?: return defaultValue
            val containsKey = mmkv.containsKey(key)
            
            if (!containsKey && defaultValue == null) {
                return null
            }
            
            val defaultSet = defaultValue?.toMutableSet() ?: mutableSetOf<String>()
            val result = mmkv.decodeStringSet(key, defaultSet)
            result
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getStringSet: $key", e)
            throwIfDebug(RuntimeException("Failed to getStringSet: $key", e))
            defaultValue
        }
    }
    
    override fun contains(key: String): Boolean {
        return try {
            getMMKV()?.containsKey(key) ?: false
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to contains: $key", e)
            throwIfDebug(RuntimeException("Failed to contains: $key", e))
            false
        }
    }
    
    override fun remove(key: String): IKVStore {
        try {
            getMMKV()?.removeValueForKey(key)
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to remove: $key", e)
            throwIfDebug(RuntimeException("Failed to remove: $key", e))
        }
        return this
    }
    
    override fun clear() {
        try {
            getMMKV()?.clearAll()
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to clear", e)
            throwIfDebug(RuntimeException("Failed to clear", e))
        }
    }
    
    override fun getAllKeys(): Set<String> {
        return try {
            val keys = getMMKV()?.allKeys()
            keys?.toSet() ?: emptySet()
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getAllKeys", e)
            throwIfDebug(RuntimeException("Failed to getAllKeys", e))
            emptySet()
        }
    }
    
    override fun getSize(): Long {
        return try {
            getMMKV()?.totalSize() ?: 0L
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to getSize", e)
            throwIfDebug(RuntimeException("Failed to getSize", e))
            0L
        }
    }
    
    override fun sync() {
        try {
            getMMKV()?.sync()
        } catch (e: Exception) {
            android.util.Log.e("MMKVStore", "Failed to sync", e)
            throwIfDebug(RuntimeException("Failed to sync", e))
        }
    }
}
