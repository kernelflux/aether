package com.kernelflux.aether.kv.api

/**
 * Key-Value 存储服务接口
 * 
 * 提供通用的键值对存储功能，支持多种数据类型
 * 
 * @author Aether Framework
 */
interface IKVStore {
    
    /**
     * 初始化存储
     * @param context 上下文（Android 平台为 Context，其他平台可能不同）
     * @param config 配置
     */
    fun init(context: Any, config: KVConfig = KVConfig.default())
    
    /**
     * 存储 String 值
     * @param key 键
     * @param value 值
     * @return 当前实例，支持链式调用
     */
    fun putString(key: String, value: String): IKVStore
    
    /**
     * 获取 String 值
     * @param key 键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    fun getString(key: String, defaultValue: String = ""): String
    
    /**
     * 存储 Int 值
     * @param key 键
     * @param value 值
     * @return 当前实例，支持链式调用
     */
    fun putInt(key: String, value: Int): IKVStore
    
    /**
     * 获取 Int 值
     * @param key 键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    fun getInt(key: String, defaultValue: Int = 0): Int
    
    /**
     * 存储 Long 值
     * @param key 键
     * @param value 值
     * @return 当前实例，支持链式调用
     */
    fun putLong(key: String, value: Long): IKVStore
    
    /**
     * 获取 Long 值
     * @param key 键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long
    
    /**
     * 存储 Float 值
     * @param key 键
     * @param value 值
     * @return 当前实例，支持链式调用
     */
    fun putFloat(key: String, value: Float): IKVStore
    
    /**
     * 获取 Float 值
     * @param key 键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float
    
    /**
     * 存储 Double 值
     * @param key 键
     * @param value 值
     * @return 当前实例，支持链式调用
     */
    fun putDouble(key: String, value: Double): IKVStore
    
    /**
     * 获取 Double 值
     * @param key 键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    fun getDouble(key: String, defaultValue: Double = 0.0): Double
    
    /**
     * 存储 Boolean 值
     * @param key 键
     * @param value 值
     * @return 当前实例，支持链式调用
     */
    fun putBoolean(key: String, value: Boolean): IKVStore
    
    /**
     * 获取 Boolean 值
     * @param key 键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    
    /**
     * 存储 ByteArray 值
     * @param key 键
     * @param value 值
     * @return 当前实例，支持链式调用
     */
    fun putBytes(key: String, value: ByteArray): IKVStore
    
    /**
     * 获取 ByteArray 值
     * @param key 键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    fun getBytes(key: String, defaultValue: ByteArray? = null): ByteArray?
    
    /**
     * 存储 Set<String> 值
     * @param key 键
     * @param value 值
     * @return 当前实例，支持链式调用
     */
    fun putStringSet(key: String, value: Set<String>): IKVStore
    
    /**
     * 获取 Set<String> 值
     * @param key 键
     * @param defaultValue 默认值
     * @return 值，如果不存在返回默认值
     */
    fun getStringSet(key: String, defaultValue: Set<String>? = null): Set<String>?
    
    /**
     * 检查键是否存在
     * @param key 键
     * @return 是否存在
     */
    fun contains(key: String): Boolean
    
    /**
     * 删除指定键
     * @param key 键
     * @return 当前实例，支持链式调用
     */
    fun remove(key: String): IKVStore
    
    /**
     * 删除所有键值对
     */
    fun clear()
    
    /**
     * 获取所有键
     * @return 所有键的集合
     */
    fun getAllKeys(): Set<String>
    
    /**
     * 获取存储大小（字节）
     * @return 存储大小
     */
    fun getSize(): Long
    
    /**
     * 同步写入（某些实现可能需要）
     */
    fun sync()
}
