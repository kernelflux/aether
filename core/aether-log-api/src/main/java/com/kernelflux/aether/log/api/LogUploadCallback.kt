package com.kernelflux.aether.log.api

/**
 * 日志上报回调接口
 * 用于监听日志上报的进度和结果
 */
interface LogUploadCallback {
    /**
     * 上报开始
     * @param totalFiles 总文件数
     * @param totalSize 总大小（字节）
     */
    fun onStart(totalFiles: Int, totalSize: Long) {}
    
    /**
     * 单个文件上报进度
     * @param fileInfo 当前上报的文件信息
     * @param currentFile 当前文件索引（从 1 开始）
     * @param totalFiles 总文件数
     */
    fun onProgress(fileInfo: LogFileInfo, currentFile: Int, totalFiles: Int) {}
    
    /**
     * 单个文件上报成功
     * @param fileInfo 已上报的文件信息
     * @param uploadId 上报 ID（由业务层返回）
     */
    fun onFileSuccess(fileInfo: LogFileInfo, uploadId: String?) {}
    
    /**
     * 单个文件上报失败
     * @param fileInfo 上报失败的文件信息
     * @param error 错误信息
     */
    fun onFileError(fileInfo: LogFileInfo, error: Throwable) {}
    
    /**
     * 所有文件上报完成
     * @param successCount 成功数量
     * @param failureCount 失败数量
     */
    fun onComplete(successCount: Int, failureCount: Int) {}
}

