// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.

#ifndef XLOGGER_APPENDER_H_
#define XLOGGER_APPENDER_H_

#include "boost/iostreams/device/mapped_file.hpp"
#include "../common/thread/condition.h"
#include "../common/thread/thread.h"
#include "../common/thread/mutex.h"
#include "../common/xlogger/xloggerbase.h"
#include "xlog_config.h"
#include "log_buffer.h"
#include <string>
#include <vector>
#include <memory>
#include <ctime>

namespace aether {
namespace xlog {

class XloggerAppender {
 public:
    static XloggerAppender* NewInstance(const XLogConfig& _config, uint64_t _max_byte_size);
    static void DelayRelease(XloggerAppender* _appender);
    static void __Release(XloggerAppender* _appender);

    void Write(const XLoggerInfo* _info, const char* _log);
    void SetMode(TAppenderMode _mode);
    void Flush();
    void FlushSync();
    void Close();
    void SetConsoleLog(bool _is_open);
    void SetMaxFileSize(uint64_t _max_byte_size);
    void SetMaxAliveDuration(long _max_time);
    
    // Get log file paths for this module
    // Returns list of log file paths (log dir and cache dir if exists)
    void GetLogFilePaths(std::vector<std::string>& _filepaths);
    
    // Get log file paths for specific time range
    // @param _days_ago 多少天前的日志（0 表示今天）
    void GetLogFilePathsByDays(std::vector<std::string>& _filepaths, int _days_ago);
    
    // Get log file paths for specific time range
    // @param _start_time 开始时间（秒时间戳）
    // @param _end_time 结束时间（秒时间戳）
    void GetLogFilePathsByTimeRange(std::vector<std::string>& _filepaths, time_t _start_time, time_t _end_time);
    
    // Get log file info (path, size, mtime) for this module
    struct LogFileInfo {
        std::string path;
        long size;
        time_t mtime;
        bool is_cache;
    };
    void GetLogFileInfos(std::vector<LogFileInfo>& _fileinfos);
    void GetLogFileInfosByDays(std::vector<LogFileInfo>& _fileinfos, int _days_ago);
    void GetLogFileInfosByTimeRange(std::vector<LogFileInfo>& _fileinfos, time_t _start_time, time_t _end_time);
    
    // Clear file list cache for this instance
    void ClearFileCache();
    

 private:
    XloggerAppender(const XLogConfig& _config, uint64_t _max_byte_size);
    
    void __WriteSync(const XLoggerInfo* _info, const char* _log);
    void __WriteAsync(const XLoggerInfo* _info, const char* _log);
    void __Log2File(const void* _data, size_t _len, bool _move_file);
    bool __OpenLogFile(const std::string& _log_dir);
    void __CloseLogFile();
    bool __WriteFile(const void* _data, size_t _len, FILE* _file);
    void __AsyncLogThread();
    void __MakeLogFileName(const timeval& _tv, const std::string& _log_dir, const char* _prefix, 
                          const std::string& _fileext, char* _filepath, unsigned int _len);
    std::string __MakeLogFileNamePrefix(const timeval& _tv, const char* _prefix);
    long __GetNextFileIndex(const std::string& _fileprefix, const std::string& _fileext);
    bool __CacheLogs();
    void __DelTimeoutFile(const std::string& _log_path);
    void __GetFileInfosByPrefix(const std::string& _logdir, const std::string& _fileprefix,
                               const std::string& _fileext, bool _is_cache,
                               std::vector<LogFileInfo>& _fileinfos);
    
    // File list cache helpers
    bool __IsCacheValid();
    void __UpdateCache(const std::vector<LogFileInfo>& _fileinfos);

 private:
    XLogConfig config_;
    LogBuffer* log_buff_ = nullptr;
    boost::iostreams::mapped_file mmap_file_;
    std::unique_ptr<Thread> thread_async_;
    Mutex mutex_buffer_async_;
    Mutex mutex_log_file_;
    FILE* logfile_ = nullptr;
    time_t openfiletime_ = 0;
    std::string current_dir_;
#ifdef DEBUG
    bool consolelog_open_ = true;
#else
    bool consolelog_open_ = false;
#endif
    bool log_close_ = true;
    Condition cond_buffer_async_;
    uint64_t max_file_size_ = 0;
    long max_alive_time_ = 10 * 24 * 60 * 60;  // 10 days in second

    time_t last_time_ = 0;
    uint64_t last_tick_ = 0;
    char last_file_path_[1024] = {0};
    
    std::unique_ptr<Thread> thread_timeout_cache_;
    std::unique_ptr<Thread> thread_moveold_;
    std::unique_ptr<Thread> thread_timeout_log_;
    
    // File list cache for current day (performance optimization)
    struct FileListCache {
        std::vector<LogFileInfo> fileinfos;
        time_t cache_time;  // Day timestamp (start of day)
        bool valid;
        FileListCache() : cache_time(0), valid(false) {}
    };
    FileListCache file_cache_;
    Mutex mutex_file_cache_;
};

}  // namespace xlog
}  // namespace aether

#endif  // XLOGGER_APPENDER_H_

