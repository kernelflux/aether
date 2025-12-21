package com.kernelflux.aether.network.okhttp

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 持久化 Cookie 管理器
 * 支持 Cookie 的持久化存储，适用于百万日活应用
 * 
 * 特性：
 * - 线程安全
 * - 持久化存储（文件系统）
 * - 自动过期清理
 * - 内存缓存加速
 */
class PersistentCookieJar(
    private val cookieStore: CookieStore
) : CookieJar {
    
    // 内存缓存，加速访问
    private val memoryCache = ConcurrentHashMap<String, List<Cookie>>()
    
    // 读写锁，保证线程安全
    private val lock = ReentrantReadWriteLock()
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        
        lock.write {
            val host = url.host
            val validCookies = cookies.filter { it.persistent || it.expiresAt > System.currentTimeMillis() }
            
            if (validCookies.isEmpty()) return
            
            // 更新内存缓存
            memoryCache[host] = validCookies
            
            // 持久化到存储
            cookieStore.save(host, validCookies)
        }
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        
        return lock.read {
            // 先从内存缓存读取
            val now = System.currentTimeMillis()
            memoryCache[host]?.let { cookies ->
                val validCookies = cookies.filter { 
                    it.expiresAt > now && (it.hostOnly && url.host == it.domain || !it.hostOnly)
                }
                if (validCookies.isNotEmpty()) {
                    return validCookies
                }
            }
            
            // 内存缓存未命中，从持久化存储读取
            val cookies = cookieStore.load(host)
            val validCookies = cookies.filter { 
                it.expiresAt > now && (it.hostOnly && url.host == it.domain || !it.hostOnly)
            }
            
            // 更新内存缓存
            if (validCookies.isNotEmpty()) {
                memoryCache[host] = validCookies
            }
            
            validCookies
        }
    }
    
    /**
     * 清除所有 Cookie
     */
    fun clear() {
        lock.write {
            memoryCache.clear()
            cookieStore.clear()
        }
    }
    
    /**
     * 清除指定域名的 Cookie
     */
    fun clear(host: String) {
        lock.write {
            memoryCache.remove(host)
            cookieStore.remove(host)
        }
    }
    
    /**
     * 清理过期的 Cookie
     */
    fun evictExpired() {
        lock.write {
            val now = System.currentTimeMillis()
            // 使用迭代器替代 removeIf（兼容 API 23）
            val iterator = memoryCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.all { it.expiresAt <= now }) {
                    iterator.remove()
                }
            }
            cookieStore.evictExpired()
        }
    }
}

/**
 * Cookie 存储接口
 */
interface CookieStore {
    /**
     * 保存 Cookie
     */
    fun save(host: String, cookies: List<Cookie>)
    
    /**
     * 加载 Cookie
     */
    fun load(host: String): List<Cookie>
    
    /**
     * 移除指定域名的 Cookie
     */
    fun remove(host: String)
    
    /**
     * 清除所有 Cookie
     */
    fun clear()
    
    /**
     * 清理过期的 Cookie
     */
    fun evictExpired()
}

/**
 * 基于文件的 Cookie 存储实现
 */
class FileCookieStore(
    private val cookieFile: File
) : CookieStore {
    
    private val lock = ReentrantReadWriteLock()
    private val cookies = ConcurrentHashMap<String, MutableList<SerializableCookie>>()
    
    init {
        loadFromFile()
    }
    
    override fun save(host: String, cookies: List<Cookie>) {
        lock.write {
            val serializableCookies = cookies.map { SerializableCookie.from(it) }
            this@FileCookieStore.cookies[host] = serializableCookies.toMutableList()
            saveToFile()
        }
    }
    
    override fun load(host: String): List<Cookie> {
        return lock.read {
            cookies[host]?.mapNotNull { it.toCookie() } ?: emptyList()
        }
    }
    
    override fun remove(host: String) {
        lock.write {
            cookies.remove(host)
            saveToFile()
        }
    }
    
    override fun clear() {
        lock.write {
            cookies.clear()
            saveToFile()
        }
    }
    
    override fun evictExpired() {
        lock.write {
            val now = System.currentTimeMillis()
            // 使用迭代器替代 removeIf（兼容 API 23）
            cookies.values.forEach { list ->
                val iterator = list.iterator()
                while (iterator.hasNext()) {
                    if (iterator.next().expiresAt <= now) {
                        iterator.remove()
                    }
                }
            }
            // 移除空列表
            val entryIterator = cookies.entries.iterator()
            while (entryIterator.hasNext()) {
                if (entryIterator.next().value.isEmpty()) {
                    entryIterator.remove()
                }
            }
            saveToFile()
        }
    }
    
    private fun loadFromFile() {
        if (!cookieFile.exists() || cookieFile.length() == 0L) return
        
        try {
            ObjectInputStream(FileInputStream(cookieFile)).use { ois ->
                @Suppress("UNCHECKED_CAST")
                val loaded = ois.readObject() as? Map<String, List<SerializableCookie>>
                loaded?.let {
                    cookies.putAll(it.mapValues { it.value.toMutableList() })
                }
            }
        } catch (e: Exception) {
            // 文件损坏，忽略
            cookieFile.delete()
        }
    }
    
    private fun saveToFile() {
        try {
            cookieFile.parentFile?.mkdirs()
            ObjectOutputStream(FileOutputStream(cookieFile)).use { oos ->
                oos.writeObject(cookies)
            }
        } catch (e: Exception) {
            // 保存失败，忽略
        }
    }
}

/**
 * 可序列化的 Cookie
 */
private data class SerializableCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
    val persistent: Boolean
) : Serializable {
    
    companion object {
        fun from(cookie: Cookie): SerializableCookie {
            return SerializableCookie(
                name = cookie.name,
                value = cookie.value,
                expiresAt = cookie.expiresAt,
                domain = cookie.domain,
                path = cookie.path,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                hostOnly = cookie.hostOnly,
                persistent = cookie.persistent
            )
        }
    }
    
    fun toCookie(): Cookie? {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .expiresAt(expiresAt)
            .path(path)
        
        if (hostOnly) {
            builder.hostOnlyDomain(domain)
        } else {
            builder.domain(domain)
        }
        
        if (secure) {
            builder.secure()
        }
        
        if (httpOnly) {
            builder.httpOnly()
        }
        
        return try {
            builder.build()
        } catch (e: Exception) {
            null
        }
    }
}
