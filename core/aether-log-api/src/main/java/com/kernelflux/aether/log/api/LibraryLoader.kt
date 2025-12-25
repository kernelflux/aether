package com.kernelflux.aether.log.api

/**
 * 自定义 so 库加载器接口
 */
interface LibraryLoader {
    /**
     * 加载 so 库
     * @param libName 库名称（不包含 "lib" 前缀和 ".so" 后缀）
     * 例如：传入 "aetherxlog" 会加载 "libaetherxlog.so"
     * 
     * @throws UnsatisfiedLinkError 如果加载失败
     */
    fun loadLibrary(libName: String)
}

