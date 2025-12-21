package com.kernelflux.aether.utils

/**
 * 集合工具类
 * 
 * 提供常用的集合操作方法
 * 
 * @author Aether Framework
 */
object CollectionUtils {
    
    /**
     * 判断集合是否为空
     */
    fun <T> isEmpty(collection: Collection<T>?): Boolean {
        return collection == null || collection.isEmpty()
    }
    
    /**
     * 判断集合是否不为空
     */
    fun <T> isNotEmpty(collection: Collection<T>?): Boolean {
        return !isEmpty(collection)
    }
    
    /**
     * 安全获取集合大小
     */
    fun <T> size(collection: Collection<T>?): Int {
        return collection?.size ?: 0
    }
    
    /**
     * 安全获取列表元素
     */
    fun <T> get(list: List<T>?, index: Int): T? {
        return if (list != null && index >= 0 && index < list.size) {
            list[index]
        } else {
            null
        }
    }
    
    /**
     * 安全获取列表第一个元素
     */
    fun <T> first(list: List<T>?): T? {
        return get(list, 0)
    }
    
    /**
     * 安全获取列表最后一个元素
     */
    fun <T> last(list: List<T>?): T? {
        return if (list != null && list.isNotEmpty()) {
            list[list.size - 1]
        } else {
            null
        }
    }
    
    /**
     * 将集合转为列表
     */
    fun <T> toList(collection: Collection<T>?): List<T> {
        return collection?.toList() ?: emptyList()
    }
    
    /**
     * 将可变参数转为列表
     */
    fun <T> listOf(vararg elements: T): List<T> {
        return elements.toList()
    }
    
    /**
     * 判断集合是否包含元素
     */
    fun <T> contains(collection: Collection<T>?, element: T): Boolean {
        return collection != null && collection.contains(element)
    }
    
    /**
     * 过滤集合
     */
    fun <T> filter(collection: Collection<T>?, predicate: (T) -> Boolean): List<T> {
        return collection?.filter(predicate) ?: emptyList()
    }
    
    /**
     * 映射集合
     */
    fun <T, R> map(collection: Collection<T>?, transform: (T) -> R): List<R> {
        return collection?.map(transform) ?: emptyList()
    }
}
