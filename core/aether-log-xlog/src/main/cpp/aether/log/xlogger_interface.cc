// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.

#include "xlogger_interface.h"
#include <functional>
#include <map>
#include <sstream>
#include <cstring>
#include <ctime>
#include <sys/time.h>
#ifdef _WIN32
#define PRIdMAX "lld"
#else
#define __STDC_FORMAT_MACROS
#include <cinttypes>
#endif
#include "../common/thread/lock.h"
#include "../common/thread/mutex.h"
#include "../common/xlogger/xlogger_category.h"
#include "xlogger_appender.h"
#include "appender.h"
#include "../common/xlogger/xloggerbase.h"
#include "verinfo.h"

using namespace aether::comm;
namespace aether {
namespace xlog {

static Mutex& GetGlobalMutex() {
    static Mutex sg_mutex;
    return sg_mutex;
}

static std::map<std::string, XloggerCategory*>& GetGlobalInstanceMap() {
    static std::map<std::string, XloggerCategory*> sg_map;
    return sg_map;
}

// Track which instances have already written header info
static std::map<std::string, bool>& GetHeaderWrittenMap() {
    static std::map<std::string, bool> sg_map;
    return sg_map;
}

// Helper function to write header information for a module instance
// Note: This should only be called once per instance, when the instance is first created
// Performance: Uses minimal locking - only checks/updates a flag, then releases lock before writing
static void WriteHeaderInfo(XloggerCategory* _category, const std::string& _nameprefix) {
    if (nullptr == _category) {
        return;
    }
    
    // Check if header has already been written for this instance (thread-safe check)
    // This is a fast path - only holds lock for map lookup, which is O(log n) where n is typically < 10
    {
        ScopedLock lock(GetGlobalMutex());
        auto& headerMap = GetHeaderWrittenMap();
        if (headerMap.find(_nameprefix) != headerMap.end() && headerMap[_nameprefix]) {
            // Header already written, skip (fast path - no header writing overhead)
            // Note: This prevents duplicate text-format header, but binary headers will still appear
            // for each log block (this is normal Mars xlog behavior)
            return;
        }
        // Mark as written before releasing lock to prevent race condition
        headerMap[_nameprefix] = true;
    } // Lock released here - header writing happens without holding lock
    
    // Write header information (lock-free, as we've already marked it as written)
    // Note: This writes TEXT-FORMAT header only once per instance.
    // Binary headers (the "gibberish" you see) are written by LogBuffer::__Reset() for each log block,
    // which is normal Mars xlog behavior and cannot be avoided.
    
    // Get global custom header info
    const char* extra_msg = appender_getExtraMSg();
    std::string sg_log_extra_msg = extra_msg ? std::string(extra_msg) : std::string("");
    
    char mark_info[512] = {0};
    struct timeval tv;
    gettimeofday(&tv, 0);
    time_t sec = tv.tv_sec;
    struct tm tm_tmp = *localtime((const time_t*)&sec);
    char tmp_time[64] = {0};
    strftime(tmp_time, sizeof(tmp_time), "%Y-%m-%d %z %H:%M:%S", &tm_tmp);
    snprintf(mark_info, sizeof(mark_info), "[%" PRIdMAX ",%" PRIdMAX "][%s]", xlogger_pid(), xlogger_tid(), tmp_time);
    
    char appender_info[728] = {0};
    snprintf(appender_info, sizeof(appender_info), "^^^^^^^^^^" __DATE__ "^^^" __TIME__ "^^^^^^^^^^%s", mark_info);
    
    _category->Write(NULL, appender_info);
    
    char logmsg[256] = {0};
    snprintf(logmsg, sizeof(logmsg), "get mmap time: 0");
    _category->Write(NULL, logmsg);
    
    _category->Write(NULL, "AETHER_PATH: " AETHER_PATH);
    _category->Write(NULL, "AETHER_REVISION: " AETHER_REVISION);
    _category->Write(NULL, "AETHER_BUILD_TIME: " AETHER_BUILD_TIME);
    
    if (strlen(AETHER_URL) > 0) {
        char url_msg[256] = {0};
        snprintf(url_msg, sizeof(url_msg), "AETHER_URL: %s", AETHER_URL);
        _category->Write(NULL, url_msg);
    }
    if (strlen(AETHER_TAG) > 0) {
        char tag_msg[256] = {0};
        snprintf(tag_msg, sizeof(tag_msg), "AETHER_BUILD_JOB: %s", AETHER_TAG);
        _category->Write(NULL, tag_msg);
    }
    
    // Output custom header info if provided
    if (!sg_log_extra_msg.empty()) {
        _category->Write(NULL, "=== Custom Header Info ===");
        std::istringstream iss(sg_log_extra_msg);
        std::string line;
        while (std::getline(iss, line)) {
            if (!line.empty()) {
                std::string formatted_line = "=== Header: " + line + " ===";
                _category->Write(NULL, formatted_line.c_str());
            }
        }
        _category->Write(NULL, "=== End Header Info ===");
    }
    
    // Note: mode and space info are instance-specific, so we skip them here
    // They can be added if needed
}

XloggerCategory* NewXloggerInstance(const XLogConfig& _config, TLogLevel _level) {
    if (_config.logdir_.empty() || _config.nameprefix_.empty()) {
        return nullptr;
    }

    XloggerCategory* category = nullptr;
    {
        ScopedLock lock(GetGlobalMutex());
        auto it = GetGlobalInstanceMap().find(_config.nameprefix_);
        if (it != GetGlobalInstanceMap().end()) {
            return it->second;
        }

        XloggerAppender* appender = XloggerAppender::NewInstance(_config, 0);

        using namespace std::placeholders;
        category = XloggerCategory::NewInstance(reinterpret_cast<uintptr_t>(appender),
                                                 std::bind(&XloggerAppender::Write, appender, _1, _2));
        category->SetLevel(_level);
        GetGlobalInstanceMap()[_config.nameprefix_] = category;
    } // Release lock before writing header to avoid deadlock
    
    // Write header information for this module instance (only once)
    // Note: Lock is released here to avoid deadlock (WriteHeaderInfo will acquire its own lock)
    WriteHeaderInfo(category, _config.nameprefix_);
    
    return category;
}

XloggerCategory* GetXloggerInstance(const char* _nameprefix) {
    if (nullptr == _nameprefix) {
        return nullptr;
    }

    ScopedLock lock(GetGlobalMutex());
    auto it = GetGlobalInstanceMap().find(_nameprefix);
    if (it != GetGlobalInstanceMap().end()) {
        return it->second;
    }

    return nullptr;
}

void ReleaseXloggerInstance(const char* _nameprefix) {
    if (nullptr == _nameprefix) {
        return;
    }

    ScopedLock lock(GetGlobalMutex());
    auto it = GetGlobalInstanceMap().find(_nameprefix);
    if (it == GetGlobalInstanceMap().end()) {
        return;
    }

    XloggerCategory* category = it->second;
    XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
    XloggerAppender::DelayRelease(appender);
    XloggerCategory::DelayRelease(category);
    GetGlobalInstanceMap().erase(it);
    
    // Also remove from header written map
    auto& headerMap = GetHeaderWrittenMap();
    headerMap.erase(_nameprefix);
}

void XloggerWrite(uintptr_t _instance_ptr, const XLoggerInfo* _info, const char* _log) {
    if (0 == _instance_ptr) {
        xlogger_Write(_info, _log);
    } else {
        XloggerCategory* category = reinterpret_cast<XloggerCategory*>(_instance_ptr);
        category->Write(_info, _log);
    }
}

bool IsEnabledFor(uintptr_t _instance_ptr, TLogLevel _level) {
    if (0 == _instance_ptr) {
        return xlogger_IsEnabledFor(_level);
    } else {
        XloggerCategory* category = reinterpret_cast<XloggerCategory*>(_instance_ptr);
        return category->IsEnabledFor(_level);
    }
}

TLogLevel GetLevel(uintptr_t _instance_ptr) {
    if (0 == _instance_ptr) {
        return xlogger_Level();
    } else {
        XloggerCategory* category = reinterpret_cast<XloggerCategory*>(_instance_ptr);
        TLogLevel level = category->GetLevel();
        return level;
    }
}

void SetLevel(uintptr_t _instance_ptr, TLogLevel _level) {
    if (0 == _instance_ptr) {
        xlogger_SetLevel(_level);
    } else {
        XloggerCategory* category = reinterpret_cast<XloggerCategory*>(_instance_ptr);
        category->SetLevel(_level);
    }
}

void SetAppenderMode(uintptr_t _instance_ptr, TAppenderMode _mode) {
    if (0 == _instance_ptr) {
        appender_setmode(_mode);
    } else {
        XloggerCategory* category = reinterpret_cast<XloggerCategory*>(_instance_ptr);
        XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
        appender->SetMode(_mode);
    }
}

void Flush(uintptr_t _instance_ptr, bool _is_sync) {
    if (0 == _instance_ptr) {
        _is_sync ? appender_flush_sync() : appender_flush();
    } else {
        XloggerCategory* category = reinterpret_cast<XloggerCategory*>(_instance_ptr);
        XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
        _is_sync ? appender->FlushSync() : appender->Flush();
    }
}

void FlushAll(bool _is_sync) {
    _is_sync ? appender_flush_sync() : appender_flush();
    ScopedLock lock(GetGlobalMutex());
    auto& xmap = GetGlobalInstanceMap();
    // loop through all categories
    for (auto it = xmap.begin(); it != xmap.end(); ++it) {
        XloggerCategory* category = it->second;
        XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
        _is_sync ? appender->FlushSync() : appender->Flush();
    }
}

void FlushModule(const char* _nameprefix, bool _is_sync) {
    if (nullptr == _nameprefix) {
        return;
    }
    
    // 性能优化：先获取实例指针，然后释放全局锁，避免在刷新时持有全局锁
    // 这样可以减少锁竞争，提高并发性能（参考微信 xlog 的设计）
    XloggerCategory* category = nullptr;
    {
        ScopedLock lock(GetGlobalMutex());
        auto it = GetGlobalInstanceMap().find(_nameprefix);
        if (it != GetGlobalInstanceMap().end()) {
            category = it->second;
        } else {
            return;  // 模块不存在，直接返回
        }
    } // 释放全局锁，刷新操作不需要全局锁保护
    
    // 刷新操作在实例级别进行，不需要全局锁
    // 每个实例有自己的 mutex_buffer_async_，不会相互阻塞
    XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
    if (appender != nullptr) {
        _is_sync ? appender->FlushSync() : appender->Flush();
    }
}

void SetConsoleLogOpen(uintptr_t _instance_ptr, bool _is_open) {
    if (0 == _instance_ptr) {
        appender_set_console_log(_is_open);
    } else {
        XloggerCategory* category = reinterpret_cast<XloggerCategory*>(_instance_ptr);
        XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
        appender->SetConsoleLog(_is_open);
    }
}

void SetMaxFileSize(uintptr_t _instance_ptr, long _max_file_size) {
    if (0 == _instance_ptr) {
        appender_set_max_file_size(_max_file_size);
    } else {
        XloggerCategory* category = reinterpret_cast<XloggerCategory*>(_instance_ptr);
        XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
        appender->SetMaxFileSize(_max_file_size);
    }
}

void SetMaxAliveTime(uintptr_t _instance_ptr, long _max_time) {
    if (0 == _instance_ptr) {
        appender_set_max_alive_duration(_max_time);
    } else {
        XloggerCategory* category = reinterpret_cast<XloggerCategory*>(_instance_ptr);
        XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
        appender->SetMaxAliveDuration(_max_time);
    }
}

void ClearFileCache(const char* _nameprefix) {
    if (nullptr == _nameprefix) {
        return;
    }
    
    ScopedLock lock(GetGlobalMutex());
    auto it = GetGlobalInstanceMap().find(_nameprefix);
    if (it != GetGlobalInstanceMap().end()) {
        XloggerCategory* category = it->second;
        XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
        if (appender != nullptr) {
            appender->ClearFileCache();
        }
    }
}

void ClearAllFileCache() {
    ScopedLock lock(GetGlobalMutex());
    auto& xmap = GetGlobalInstanceMap();
    for (auto it = xmap.begin(); it != xmap.end(); ++it) {
        XloggerCategory* category = it->second;
        XloggerAppender* appender = reinterpret_cast<XloggerAppender*>(category->GetAppender());
        if (appender != nullptr) {
            appender->ClearFileCache();
        }
    }
}


}  // namespace xlog
}  // namespace aether

