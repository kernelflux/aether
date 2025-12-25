package com.kernelflux.aether.common

import androidx.core.graphics.toColorInt

/**
 * @author: QT
 * @date: 2025/12/24
 */
object ColorHelper {

    @JvmStatic
    fun parseColor(colorStr: String?, defaultColor: Int): Int {
        return colorStr?.let {
            return try {
                it.trim().toColorInt()
            } catch (_: Throwable) {
                defaultColor
            }
        } ?: defaultColor
    }

    @JvmStatic
    fun parseColor(colorStr: String?): Int {
        return parseColor(colorStr, 0)
    }


}