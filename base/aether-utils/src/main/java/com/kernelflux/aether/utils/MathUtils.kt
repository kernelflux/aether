package com.kernelflux.aether.utils

/**
 * 数学工具类
 * 
 * 提供常用的数学计算方法
 * 
 * @author Aether Framework
 */
object MathUtils {
    
    /**
     * 限制值在指定范围内
     */
    fun clamp(value: Int, min: Int, max: Int): Int {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
    
    /**
     * 限制值在指定范围内（Float）
     */
    fun clamp(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
    
    /**
     * 限制值在指定范围内（Double）
     */
    fun clamp(value: Double, min: Double, max: Double): Double {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
    
    /**
     * 线性插值
     */
    fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }
    
    /**
     * 线性插值（Double）
     */
    fun lerp(start: Double, end: Double, fraction: Double): Double {
        return start + (end - start) * fraction
    }
    
    /**
     * 将值映射到新范围
     */
    fun map(value: Float, fromMin: Float, fromMax: Float, toMin: Float, toMax: Float): Float {
        val fromRange = fromMax - fromMin
        val toRange = toMax - toMin
        if (fromRange == 0f) return toMin
        val scale = toRange / fromRange
        return toMin + (value - fromMin) * scale
    }
    
    /**
     * 判断两个浮点数是否近似相等
     */
    fun equals(a: Float, b: Float, epsilon: Float = 0.0001f): Boolean {
        return Math.abs(a - b) < epsilon
    }
    
    /**
     * 判断两个双精度数是否近似相等
     */
    fun equals(a: Double, b: Double, epsilon: Double = 0.0001): Boolean {
        return Math.abs(a - b) < epsilon
    }
    
    /**
     * 四舍五入
     */
    fun round(value: Double): Long {
        return Math.round(value)
    }
    
    /**
     * 向上取整
     */
    fun ceil(value: Double): Double {
        return Math.ceil(value)
    }
    
    /**
     * 向下取整
     */
    fun floor(value: Double): Double {
        return Math.floor(value)
    }
}
