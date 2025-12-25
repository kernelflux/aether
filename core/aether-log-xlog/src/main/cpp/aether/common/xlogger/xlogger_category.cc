// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.

#include "xlogger_category.h"
#include <functional>
#include "../thread/thread.h"

namespace aether {
namespace comm {

XloggerCategory* XloggerCategory::NewInstance(
    uintptr_t _appender,
    std::function<void(const XLoggerInfo* _info, const char* _log)> _appender_func) {
    return new XloggerCategory(_appender, _appender_func);
}

void XloggerCategory::DelayRelease(XloggerCategory* _category) {
    Thread(std::bind(&__Release, _category)).start_after(5000);
}

void XloggerCategory::__Release(XloggerCategory* _category) {
    delete _category;
}

XloggerCategory::XloggerCategory(uintptr_t _appender,
                                 std::function<void(const XLoggerInfo* _info, const char* _log)> _appender_func)
: appender_(_appender), appender_func_(_appender_func) {
}

intptr_t XloggerCategory::GetAppender() {
    return appender_;
}

TLogLevel XloggerCategory::GetLevel() {
    return level_;
}

void XloggerCategory::SetLevel(TLogLevel _level) {
    level_ = _level;
}

bool XloggerCategory::IsEnabledFor(TLogLevel _level) {
    return level_ <= _level;
}

void XloggerCategory::VPrint(const XLoggerInfo* _info, const char* _format, va_list _list) {
    if (NULL == _format) {
        XLoggerInfo* info = (XLoggerInfo*)_info;
        info->level = kLevelFatal;
        __WriteImpl(_info, "NULL == _format");
    } else {
        char temp[4096] = {'\0'};
        vsnprintf(temp, 4096, _format, _list);
        __WriteImpl(_info, temp);
    }
}

void XloggerCategory::Print(const XLoggerInfo* _info, const char* _format, ...) {
    va_list valist;
    va_start(valist, _format);
    VPrint(_info, _format, valist);
    va_end(valist);
}

void XloggerCategory::Write(const XLoggerInfo* _info, const char* _log) {
    __WriteImpl(_info, _log);
}

void XloggerCategory::__WriteImpl(const XLoggerInfo* _info, const char* _log) {
    if (!appender_func_) {
        return;
    }
    if (_info && _info->level < level_) {
        return;
    }

    if (_info && -1 == _info->pid && -1 == _info->tid && -1 == _info->maintid) {
        XLoggerInfo* info = (XLoggerInfo*)_info;
        info->pid = xlogger_pid();
        info->tid = xlogger_tid();
        info->maintid = xlogger_maintid();
    }

    if (NULL == _log) {
        if (_info) {
            XLoggerInfo* info = (XLoggerInfo*)_info;
            info->level = kLevelFatal;
        }
        appender_func_(_info, "NULL == _log");
    } else {
        appender_func_(_info, _log);
    }
}

}  // namespace comm
}  // namespace aether

