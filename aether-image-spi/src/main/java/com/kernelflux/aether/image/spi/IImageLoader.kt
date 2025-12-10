package com.kernelflux.aether.image.spi

import android.widget.ImageView

/**
 * 图片加载服务接口
 * 
 * @author Aether Framework
 */
interface IImageLoader {
    
    /**
     * 加载图片
     * @param imageView 目标ImageView
     * @param url 图片URL
     * @param placeholder 占位图资源ID
     * @param error 错误图资源ID
     */
    fun load(
        imageView: ImageView,
        url: String?,
        placeholder: Int = 0,
        error: Int = 0
    )
    
    /**
     * 加载圆形图片
     */
    fun loadCircle(
        imageView: ImageView,
        url: String?,
        placeholder: Int = 0,
        error: Int = 0
    )
    
    /**
     * 加载圆角图片
     * @param radius 圆角半径（dp）
     */
    fun loadRound(
        imageView: ImageView,
        url: String?,
        radius: Float = 8f,
        placeholder: Int = 0,
        error: Int = 0
    )
    
    /**
     * 清除内存缓存
     */
    fun clearMemoryCache()
    
    /**
     * 清除磁盘缓存
     */
    fun clearDiskCache()
}

