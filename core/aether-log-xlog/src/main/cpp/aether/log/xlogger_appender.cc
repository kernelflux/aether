// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.

#include "xlogger_appender.h"
#include "appender.h"
#include "../common/thread/thread.h"
#include "../common/thread/lock.h"
#include "../common/autobuffer.h"
#include "../common/ptrbuffer.h"
#include "../common/xlogger/xloggerbase.h"
#include "../common/time_utils.h"
#include "../common/strutil.h"
#include "../common/mmap_util.h"
#include "../common/tickcount.h"
#include <cstdio>
#include <cstring>
#include <ctime>
#include <sys/time.h>
#include <memory>
#include <algorithm>
#include <boost/filesystem.hpp>
#include <boost/iostreams/device/mapped_file.hpp>

// Forward declarations for functions in global namespace
extern void log_formater(const XLoggerInfo* _info, const char* _logbody, PtrBuffer& _log);
extern void ConsoleLog(const XLoggerInfo* _info, const char* _log);

namespace aether {
namespace xlog {

static const unsigned int kBufferBlockLength = 150 * 1024;

XloggerAppender* XloggerAppender::NewInstance(const XLogConfig& _config, uint64_t _max_byte_size) {
    return new XloggerAppender(_config, _max_byte_size);
}

void XloggerAppender::DelayRelease(XloggerAppender* _appender) {
    Thread(std::bind(&XloggerAppender::__Release, _appender)).start_after(5000);
}

void XloggerAppender::__Release(XloggerAppender* _appender) {
    if (_appender) {
        _appender->Close();
        delete _appender;
    }
}

XloggerAppender::XloggerAppender(const XLogConfig& _config, uint64_t _max_byte_size)
    : config_(_config)
    , log_close_(true)
    , max_file_size_(_max_byte_size) {
    // Initialize file cache
    file_cache_.cache_time = 0;
    file_cache_.valid = false;
    
    // Initialize based on config
    boost::filesystem::create_directories(config_.logdir_);
    if (!config_.cachedir_.empty()) {
        boost::filesystem::create_directories(config_.cachedir_);
    }
    
    // Open mmap file or create buffer
    char mmap_file_path[512] = {0};
    std::string cache_dir = config_.cachedir_.empty() ? config_.logdir_ : config_.cachedir_;
    snprintf(mmap_file_path, sizeof(mmap_file_path), "%s/%s.mmap3", cache_dir.c_str(), config_.nameprefix_.c_str());
    
    bool use_mmap = false;
    if (OpenMmapFile(mmap_file_path, kBufferBlockLength, mmap_file_)) {
        log_buff_ = new LogBuffer(mmap_file_.data(), kBufferBlockLength, config_.is_compress_, config_.pub_key_.c_str());
        use_mmap = true;
    } else {
        char* buffer = new char[kBufferBlockLength];
        log_buff_ = new LogBuffer(buffer, kBufferBlockLength, config_.is_compress_, config_.pub_key_.c_str());
        use_mmap = false;
    }
    
    // Start async thread if needed
    if (config_.mode_ == kAppednerAsync) {
        thread_async_.reset(new Thread(std::bind(&XloggerAppender::__AsyncLogThread, this)));
        thread_async_->start();
    }
    
    log_close_ = false;
}

void XloggerAppender::Write(const XLoggerInfo* _info, const char* _log) {
    if (log_close_) return;
    
    if (consolelog_open_) {
        ConsoleLog(_info, _log);
    }
    
    if (config_.mode_ == kAppednerSync) {
        __WriteSync(_info, _log);
    } else {
        __WriteAsync(_info, _log);
    }
}

void XloggerAppender::__WriteSync(const XLoggerInfo* _info, const char* _log) {
    char temp[16 * 1024] = {0};
    PtrBuffer log_buff(temp, 0, sizeof(temp));
    log_formater(_info, _log, log_buff);
    
    AutoBuffer tmp_buff;
    if (!log_buff_->Write(log_buff.Ptr(), log_buff.Length(), tmp_buff)) {
        return;
    }
    
    __Log2File(tmp_buff.Ptr(), tmp_buff.Length(), false);
}

void XloggerAppender::__WriteAsync(const XLoggerInfo* _info, const char* _log) {
    ScopedLock lock(mutex_buffer_async_);
    if (log_buff_ == nullptr) return;
    
    char temp[16 * 1024] = {0};
    PtrBuffer log_buff(temp, 0, sizeof(temp));
    log_formater(_info, _log, log_buff);
    
    if (!log_buff_->Write(log_buff.Ptr(), (unsigned int)log_buff.Length())) {
        return;
    }
    
    // 自动刷新触发条件（性能优化）：
    // 1. 缓冲区达到 1/3 大小（约 50KB）- 避免频繁刷新影响性能
    // 2. FATAL 级别日志 - 确保严重错误立即写入
    // 注意：这是自动触发，不会因为少量日志就频繁刷新
    if (log_buff_->GetData().Length() >= kBufferBlockLength * 1 / 3 || 
        (_info && _info->level == kLevelFatal)) {
        cond_buffer_async_.notifyAll(lock);
    }
}

void XloggerAppender::__AsyncLogThread() {
    while (true) {
        ScopedLock lock_buffer(mutex_buffer_async_);
        
        if (log_buff_ == nullptr) break;
        
        AutoBuffer tmp;
        log_buff_->Flush(tmp);
        lock_buffer.unlock();
        
        if (tmp.Ptr()) {
            __Log2File(tmp.Ptr(), tmp.Length(), true);
        }
        
        if (log_close_) break;
        
        ScopedLock lock_wait(mutex_buffer_async_);
        cond_buffer_async_.wait(lock_wait, 15 * 60 * 1000);
    }
}

void XloggerAppender::__Log2File(const void* _data, size_t _len, bool _move_file) {
    if (NULL == _data || 0 == _len || config_.logdir_.empty()) {
        return;
    }
    
    ScopedLock lock_file(mutex_log_file_);
    
    if (config_.cachedir_.empty()) {
        if (__OpenLogFile(config_.logdir_)) {
            __WriteFile(_data, _len, logfile_);
            if (config_.mode_ == kAppednerAsync) {
                __CloseLogFile();
            }
        }
        return;
    }
    
    struct timeval tv;
    gettimeofday(&tv, NULL);
    char logcachefilepath[1024] = {0};
    
    __MakeLogFileName(tv, config_.cachedir_, config_.nameprefix_.c_str(), std::string("xlog"), logcachefilepath, 1024);
    
    bool cache_logs = __CacheLogs();
    if ((cache_logs || boost::filesystem::exists(logcachefilepath)) && __OpenLogFile(config_.cachedir_)) {
        __WriteFile(_data, _len, logfile_);
        if (config_.mode_ == kAppednerAsync) {
            __CloseLogFile();
        }
        
        if (cache_logs || !_move_file) {
            return;
        }
        
        char logfilepath[1024] = {0};
        __MakeLogFileName(tv, config_.logdir_, config_.nameprefix_.c_str(), std::string("xlog"), logfilepath, 1024);
        // TODO: Implement __AppendFile
        // if (__AppendFile(logcachefilepath, logfilepath)) {
        //     if (config_.mode_ == kAppednerSync) {
        //         __CloseLogFile();
        //     }
        //     boost::filesystem::remove(logcachefilepath);
        // }
        return;
    }
    
    bool write_success = false;
    bool open_success = __OpenLogFile(config_.logdir_);
    if (open_success) {
        write_success = __WriteFile(_data, _len, logfile_);
        if (config_.mode_ == kAppednerAsync) {
            __CloseLogFile();
        }
    }
    
    if (!write_success) {
        if (open_success && config_.mode_ == kAppednerSync) {
            __CloseLogFile();
        }
        
        if (__OpenLogFile(config_.cachedir_)) {
            __WriteFile(_data, _len, logfile_);
            if (config_.mode_ == kAppednerAsync) {
                __CloseLogFile();
            }
        }
    }
}

bool XloggerAppender::__OpenLogFile(const std::string& _log_dir) {
    if (config_.logdir_.empty()) return false;
    
    struct timeval tv;
    gettimeofday(&tv, NULL);
    
    if (logfile_ != nullptr) {
        time_t sec = tv.tv_sec;
        tm tcur = *localtime((const time_t*)&sec);
        tm filetm = *localtime(&openfiletime_);
        
        if (filetm.tm_year == tcur.tm_year && filetm.tm_mon == tcur.tm_mon && 
            filetm.tm_mday == tcur.tm_mday && current_dir_ == _log_dir) {
            return true;
        }
        
        fclose(logfile_);
        logfile_ = nullptr;
    }
    
    char logfilepath[1024] = {0};
    __MakeLogFileName(tv, _log_dir, config_.nameprefix_.c_str(), std::string("xlog"), logfilepath, 1024);
    
    openfiletime_ = tv.tv_sec;
    current_dir_ = _log_dir;
    
    logfile_ = fopen(logfilepath, "ab");
    if (logfile_ == nullptr) {
        // Log error
        return false;
    }
    
    return true;
}

void XloggerAppender::__CloseLogFile() {
    if (logfile_ == nullptr) return;
    
    openfiletime_ = 0;
    fclose(logfile_);
    logfile_ = nullptr;
}

bool XloggerAppender::__WriteFile(const void* _data, size_t _len, FILE* _file) {
    if (_file == nullptr) {
        return false;
    }
    
    long before_len = ftell(_file);
    if (before_len < 0) return false;
    
    if (1 != fwrite(_data, _len, 1, _file)) {
        int err = ferror(_file);
        ftruncate(fileno(_file), before_len);
        fseek(_file, 0, SEEK_END);
        return false;
    }
    
    return true;
}

std::string XloggerAppender::__MakeLogFileNamePrefix(const timeval& _tv, const char* _prefix) {
    time_t sec = _tv.tv_sec;
    tm tcur = *localtime((const time_t*)&sec);
    
    char temp[64] = {0};
    snprintf(temp, 64, "_%d%02d%02d", 1900 + tcur.tm_year, 1 + tcur.tm_mon, tcur.tm_mday);
    
    std::string filenameprefix = _prefix;
    filenameprefix += temp;
    
    return filenameprefix;
}

long XloggerAppender::__GetNextFileIndex(const std::string& _fileprefix, const std::string& _fileext) {
    // Simplified implementation - can be enhanced later
    return 0;
}

void XloggerAppender::__MakeLogFileName(const timeval& _tv, const std::string& _log_dir, const char* _prefix,
                                        const std::string& _fileext, char* _filepath, unsigned int _len) {
    long index = 0;
    std::string logfilenameprefix = __MakeLogFileNamePrefix(_tv, _prefix);
    if (max_file_size_ > 0) {
        index = __GetNextFileIndex(logfilenameprefix, _fileext);
    }
    
    std::string logfilepath = _log_dir;
    logfilepath += "/";
    logfilepath += logfilenameprefix;
    
    if (index > 0) {
        char temp[24] = {0};
        snprintf(temp, 24, "_%ld", index);
        logfilepath += temp;
    }
    
    logfilepath += ".";
    logfilepath += _fileext;
    
    strncpy(_filepath, logfilepath.c_str(), _len - 1);
    _filepath[_len - 1] = '\0';
}

bool XloggerAppender::__CacheLogs() {
    if (config_.cachedir_.empty() || config_.cache_days_ <= 0) {
        return false;
    }
    
    struct timeval tv;
    gettimeofday(&tv, NULL);
    char logfilepath[1024] = {0};
    __MakeLogFileName(tv, config_.logdir_, config_.nameprefix_.c_str(), std::string("xlog"), logfilepath, 1024);
    if (boost::filesystem::exists(logfilepath)) {
        return false;
    }
    
    // Check available space
    static const uintmax_t kAvailableSizeThreshold = (uintmax_t)1 * 1024 * 1024 * 1024;  // 1G
    boost::filesystem::space_info info = boost::filesystem::space(config_.cachedir_);
    if (info.available < kAvailableSizeThreshold) {
        return false;
    }
    
    return true;
}

void XloggerAppender::__DelTimeoutFile(const std::string& _log_path) {
    // Implementation can be added later
}

void XloggerAppender::SetMode(TAppenderMode _mode) {
    config_.mode_ = _mode;
}

void XloggerAppender::Flush() {
    // 手动刷新：通知异步线程处理缓冲区
    // 注意：这是用户主动调用的，用于强制刷新缓冲区数据到文件
    // 与自动刷新不同，这里会立即触发刷新，即使缓冲区未满
    ScopedLock lock(mutex_buffer_async_);
    cond_buffer_async_.notifyAll(lock);
}

void XloggerAppender::FlushSync() {
    ScopedLock lock_buffer(mutex_buffer_async_);
    
    if (log_buff_ == nullptr) return;
    
    // 检查缓冲区是否有数据，避免重复刷新
    // LogBuffer::Flush() 会调用 __Clear() 清空缓冲区，所以不会重复写入
    if (log_buff_->GetData().Length() == 0) {
        // 缓冲区为空，说明已经刷新过了，直接返回
        // 这样可以避免重复落盘（例如：异步线程已经刷新，或者之前已经手动刷新过）
        return;
    }
    
    AutoBuffer tmp;
    log_buff_->Flush(tmp);  // Flush 会调用 __Clear() 清空缓冲区
    lock_buffer.unlock();
    
    if (tmp.Ptr()) {
        __Log2File(tmp.Ptr(), tmp.Length(), false);
    }
}

void XloggerAppender::Close() {
    if (log_close_) return;
    
    log_close_ = true;
    {
        ScopedLock lock(mutex_buffer_async_);
        cond_buffer_async_.notifyAll(lock);
    }
    
    if (config_.mode_ == kAppednerAsync && thread_async_) {
        thread_async_->join();
        thread_async_.reset();
    }
    
    __CloseLogFile();
    
    if (log_buff_) {
        delete log_buff_;
        log_buff_ = nullptr;
    }
}

void XloggerAppender::SetConsoleLog(bool _is_open) {
    consolelog_open_ = _is_open;
}

void XloggerAppender::SetMaxFileSize(uint64_t _max_byte_size) {
    max_file_size_ = _max_byte_size;
}

void XloggerAppender::SetMaxAliveDuration(long _max_time) {
    max_alive_time_ = _max_time;
}

void XloggerAppender::GetLogFilePaths(std::vector<std::string>& _filepaths) {
    std::vector<LogFileInfo> infos;
    GetLogFileInfos(infos);
    for (const auto& info : infos) {
        _filepaths.push_back(info.path);
    }
}

void XloggerAppender::GetLogFilePathsByDays(std::vector<std::string>& _filepaths, int _days_ago) {
    std::vector<LogFileInfo> infos;
    GetLogFileInfosByDays(infos, _days_ago);
    for (const auto& info : infos) {
        _filepaths.push_back(info.path);
    }
}

void XloggerAppender::GetLogFilePathsByTimeRange(std::vector<std::string>& _filepaths, time_t _start_time, time_t _end_time) {
    std::vector<LogFileInfo> infos;
    GetLogFileInfosByTimeRange(infos, _start_time, _end_time);
    for (const auto& info : infos) {
        _filepaths.push_back(info.path);
    }
}

void XloggerAppender::GetLogFileInfos(std::vector<LogFileInfo>& _fileinfos) {
    _fileinfos.clear();
    
    if (config_.logdir_.empty() && config_.cachedir_.empty()) {
        return;
    }
    
    // Check cache first (performance optimization)
    {
        ScopedLock lock(mutex_file_cache_);
        if (__IsCacheValid()) {
            _fileinfos = file_cache_.fileinfos;
            return;  // Return cached result
        }
    }
    
    // Cache invalid or not exists, scan file system
    struct timeval tv;
    gettimeofday(&tv, NULL);
    
    // Get current day's log file info
    char logfilepath[1024] = {0};
    if (!config_.logdir_.empty()) {
        __MakeLogFileName(tv, config_.logdir_, config_.nameprefix_.c_str(), std::string("xlog"), logfilepath, 1024);
        if (boost::filesystem::exists(logfilepath)) {
            try {
                LogFileInfo info;
                info.path = std::string(logfilepath);
                info.size = boost::filesystem::file_size(logfilepath);
                info.mtime = boost::filesystem::last_write_time(logfilepath);
                info.is_cache = false;
                _fileinfos.push_back(info);
            } catch (const boost::filesystem::filesystem_error&) {
                // Skip files that can't be accessed (may have been deleted)
                // Clear cache if file access fails (file may have been deleted)
                {
                    ScopedLock lock(mutex_file_cache_);
                    file_cache_.valid = false;
                }
            }
        }
    }
    
    // Get cache dir log file info (if exists)
    if (!config_.cachedir_.empty()) {
        char cachefilepath[1024] = {0};
        __MakeLogFileName(tv, config_.cachedir_, config_.nameprefix_.c_str(), std::string("xlog"), cachefilepath, 1024);
        if (boost::filesystem::exists(cachefilepath)) {
            try {
                LogFileInfo info;
                info.path = std::string(cachefilepath);
                info.size = boost::filesystem::file_size(cachefilepath);
                info.mtime = boost::filesystem::last_write_time(cachefilepath);
                info.is_cache = true;
                _fileinfos.push_back(info);
            } catch (const boost::filesystem::filesystem_error&) {
                // Skip files that can't be accessed (may have been deleted)
                // Clear cache if file access fails (file may have been deleted)
                {
                    ScopedLock lock(mutex_file_cache_);
                    file_cache_.valid = false;
                }
            }
        }
    }
    
    // Sort by modification time (newest first)
    std::sort(_fileinfos.begin(), _fileinfos.end(), [](const LogFileInfo& a, const LogFileInfo& b) {
        return a.mtime > b.mtime;
    });
    
    // Update cache
    {
        ScopedLock lock(mutex_file_cache_);
        __UpdateCache(_fileinfos);
    }
}

void XloggerAppender::GetLogFileInfosByDays(std::vector<LogFileInfo>& _fileinfos, int _days_ago) {
    _fileinfos.clear();
    
    if (config_.logdir_.empty() && config_.cachedir_.empty()) {
        return;
    }
    
    // Validate days_ago (prevent negative or too large values)
    if (_days_ago < 0) {
        _days_ago = 0;  // Treat negative as today
    }
    if (_days_ago > 365) {
        return;  // Limit to 1 year
    }
    
    struct timeval tv;
    gettimeofday(&tv, NULL);
    tv.tv_sec -= _days_ago * (24 * 60 * 60);
    
    // Get log files for the specified day
    std::string fileprefix = __MakeLogFileNamePrefix(tv, config_.nameprefix_.c_str());
    std::string fileext = "xlog";
    
    // Scan log dir
    if (!config_.logdir_.empty()) {
        __GetFileInfosByPrefix(config_.logdir_, fileprefix, fileext, false, _fileinfos);
    }
    
    // Scan cache dir
    if (!config_.cachedir_.empty()) {
        __GetFileInfosByPrefix(config_.cachedir_, fileprefix, fileext, true, _fileinfos);
    }
    
    // Sort by modification time (newest first)
    std::sort(_fileinfos.begin(), _fileinfos.end(), [](const LogFileInfo& a, const LogFileInfo& b) {
        return a.mtime > b.mtime;
    });
}

void XloggerAppender::GetLogFileInfosByTimeRange(std::vector<LogFileInfo>& _fileinfos, time_t _start_time, time_t _end_time) {
    _fileinfos.clear();
    
    if (config_.logdir_.empty() && config_.cachedir_.empty()) {
        return;
    }
    
    // Validate time range
    if (_start_time > _end_time) {
        return;  // Invalid time range
    }
    
    // Limit time range to prevent excessive scanning (max 30 days)
    time_t max_range = 30 * 24 * 60 * 60;  // 30 days in seconds
    if (_end_time - _start_time > max_range) {
        _start_time = _end_time - max_range;  // Adjust start_time to limit range
    }
    
    // Calculate days between start and end
    time_t start_day = _start_time / (24 * 60 * 60) * (24 * 60 * 60);  // Start of day
    time_t end_day = _end_time / (24 * 60 * 60) * (24 * 60 * 60);      // Start of day
    int days = (int)((end_day - start_day) / (24 * 60 * 60)) + 1;
    
    // Limit days to prevent excessive scanning
    if (days > 30) {
        days = 30;
    }
    
    for (int i = 0; i <= days; ++i) {
        time_t day_time = start_day + i * (24 * 60 * 60);
        
        // Convert to timeval for __MakeLogFileNamePrefix
        struct timeval tv;
        tv.tv_sec = day_time;
        tv.tv_usec = 0;
        
        std::string fileprefix = __MakeLogFileNamePrefix(tv, config_.nameprefix_.c_str());
        std::string fileext = "xlog";
        
        // Scan log dir
        if (!config_.logdir_.empty()) {
            __GetFileInfosByPrefix(config_.logdir_, fileprefix, fileext, false, _fileinfos);
        }
        
        // Scan cache dir
        if (!config_.cachedir_.empty()) {
            __GetFileInfosByPrefix(config_.cachedir_, fileprefix, fileext, true, _fileinfos);
        }
    }
    
    // Filter by time range and sort
    _fileinfos.erase(
        std::remove_if(_fileinfos.begin(), _fileinfos.end(),
            [_start_time, _end_time](const LogFileInfo& info) {
                return info.mtime < _start_time || info.mtime > _end_time;
            }),
        _fileinfos.end()
    );
    
    // Sort by modification time (descending, newest first)
    std::sort(_fileinfos.begin(), _fileinfos.end(), [](const LogFileInfo& a, const LogFileInfo& b) {
        return a.mtime > b.mtime;
    });
}

// Helper function to get file infos by prefix
void XloggerAppender::__GetFileInfosByPrefix(const std::string& _logdir, const std::string& _fileprefix,
                                             const std::string& _fileext, bool _is_cache,
                                             std::vector<LogFileInfo>& _fileinfos) {
    boost::filesystem::path path(_logdir);
    if (!boost::filesystem::is_directory(path)) {
        return;
    }
    
    boost::filesystem::directory_iterator end_iter;
    for (boost::filesystem::directory_iterator iter(path); iter != end_iter; ++iter) {
        if (boost::filesystem::is_regular_file(iter->status())) {
            std::string filename = iter->path().filename().string();
            // Check if filename starts with prefix and ends with .{fileext}
            std::string expected_ext = "." + _fileext;
            if (filename.length() >= _fileprefix.length() + expected_ext.length() &&
                filename.substr(0, _fileprefix.length()) == _fileprefix &&
                filename.substr(filename.length() - expected_ext.length()) == expected_ext) {
                boost::filesystem::path filepath = boost::filesystem::path(_logdir) / filename;
                try {
                    LogFileInfo info;
                    info.path = filepath.string();
                    info.size = boost::filesystem::file_size(filepath);
                    info.mtime = boost::filesystem::last_write_time(filepath);
                    info.is_cache = _is_cache;
                    _fileinfos.push_back(info);
                } catch (const boost::filesystem::filesystem_error&) {
                    // Skip files that can't be accessed (may have been deleted)
                    continue;
                }
            }
        }
    }
}

void XloggerAppender::ClearFileCache() {
    ScopedLock lock(mutex_file_cache_);
    file_cache_.fileinfos.clear();
    file_cache_.cache_time = 0;
    file_cache_.valid = false;
}

bool XloggerAppender::__IsCacheValid() {
    if (!file_cache_.valid) {
        return false;
    }
    
    // Check if cache is for current day
    struct timeval tv;
    gettimeofday(&tv, NULL);
    time_t current_day = tv.tv_sec / (24 * 60 * 60) * (24 * 60 * 60);
    
    if (file_cache_.cache_time != current_day) {
        return false;  // Cache is for a different day
    }
    
    // Defensive check: verify at least one cached file still exists
    // This prevents returning stale cache if files were deleted externally
    // Full verification would be too expensive, so we do a lightweight check
    if (!file_cache_.fileinfos.empty()) {
        bool has_valid_file = false;
        for (const auto& info : file_cache_.fileinfos) {
            if (boost::filesystem::exists(info.path)) {
                has_valid_file = true;
                break;
            }
        }
        // If no cached files exist, cache is invalid
        if (!has_valid_file) {
            return false;
        }
    }
    
    return true;
}

void XloggerAppender::__UpdateCache(const std::vector<LogFileInfo>& _fileinfos) {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    time_t current_day = tv.tv_sec / (24 * 60 * 60) * (24 * 60 * 60);
    
    file_cache_.fileinfos = _fileinfos;
    file_cache_.cache_time = current_day;
    file_cache_.valid = true;
}

// Removed: GetLogFileInfosByTimeRangeWithCallback - progress callback not needed
// Progress callbacks add complexity without significant benefit for fast file scanning operations
// The scanning operation is typically very fast (milliseconds to seconds), so progress callbacks
// are unnecessary overhead. If needed, business layer can implement progress tracking by querying
// in smaller batches (e.g., 7 days at a time).

}  // namespace xlog
}  // namespace aether

