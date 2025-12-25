// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.


#include <cassert>
#include <cstdio>
#include <climits>
#include <cstring>
#include <algorithm>

#include "xloggerbase.h"
#include "loginfo_extract.h"
#include "ptrbuffer.h"

#ifdef _WIN32
#define PRIdMAX "lld"
#define snprintf _snprintf
#else
#define __STDC_FORMAT_MACROS
#include <cinttypes>
#endif

void log_formater(const XLoggerInfo* _info, const char* _logbody, PtrBuffer& _log) {
    static const char* levelStrings[] = {
        "V",
        "D",  // debug
        "I",  // info
        "W",  // warn
        "E",  // error
        "F"  // fatal
    };

    assert((unsigned int)_log.Pos() == _log.Length());

    static int error_count = 0;
    static int error_size = 0;

    if (_log.MaxLength() <= _log.Length() + 5 * 1024) {  // allowd len(_log) <= 11K(16K - 5K)
        ++error_count;
        error_size = (int)strnlen(_logbody, 1024 * 1024);

        if (_log.MaxLength() >= _log.Length() + 128) {
            int ret = snprintf((char*)_log.PosPtr(), 1024, "[F]log_size <= 5*1024, err(%d, %d)\n", error_count, error_size);  // **CPPLINT SKIP**
            _log.Length(_log.Pos() + ret, _log.Length() + ret);
            _log.Write("");

            error_count = 0;
            error_size = 0;
        }

        assert(false);
        return;
    }

    if (NULL != _info) {
        const char* filename = ExtractFileName(_info->filename);
        char strFuncName [128] = {0};
        ExtractFunctionName(_info->func_name, strFuncName, sizeof(strFuncName));

        // 优化时间格式：使用标准格式 YYYY-MM-DD HH:mm:ss.SSS（去掉时区偏移）
        char temp_time[32] = {0};
        if (0 != _info->timeval.tv_sec) {
            time_t sec = _info->timeval.tv_sec;
            tm tm = *localtime((const time_t*)&sec);
            snprintf(temp_time, sizeof(temp_time), "%04d-%02d-%02d %02d:%02d:%02d.%03ld",
                     1900 + tm.tm_year, 1 + tm.tm_mon, tm.tm_mday,
                     tm.tm_hour, tm.tm_min, tm.tm_sec, _info->timeval.tv_usec / 1000);
        }

        // 优化日志格式：参考 Logback/Log4j2 和 Android Logcat 的清晰格式
        // 格式：时间 [PID:TID*] LEVEL/TAG 文件名:行号 - 消息
        // 示例：2025-12-22 18:56:27.897 [25449:25449*] D/Account LogActivity.kt:212 - 用户登录请求
        
        const char* tag = _info->tag && strlen(_info->tag) > 0 ? _info->tag : "-";
        const char* file = filename && strlen(filename) > 0 ? filename : "-";
        const char* func = strFuncName[0] != '\0' ? strFuncName : "-";
        int line = _info->line > 0 ? _info->line : 0;
        const char* mainThreadMark = _info->tid == _info->maintid ? "*" : "";
        
        // 构建位置信息（文件名:行号 或 函数名:行号）
        char location[256] = {0};
        if (line > 0) {
            if (file[0] != '-' && strlen(file) > 0) {
                snprintf(location, sizeof(location), "%s:%d", file, line);
            } else if (func[0] != '-' && strlen(func) > 0) {
                snprintf(location, sizeof(location), "%s:%d", func, line);
            } else {
                snprintf(location, sizeof(location), ":%d", line);
            }
        } else {
            if (file[0] != '-' && strlen(file) > 0) {
                snprintf(location, sizeof(location), "%s", file);
            } else if (func[0] != '-' && strlen(func) > 0) {
                snprintf(location, sizeof(location), "%s", func);
            } else {
                location[0] = '\0';
            }
        }

        int ret = snprintf((char*)_log.PosPtr(), 1024, 
                           "%s [%" PRIdMAX ":%" PRIdMAX "%s] %s/%s %s - ",  // **CPPLINT SKIP**
                           temp_time,
                           _info->pid, _info->tid, mainThreadMark,
                           _logbody ? levelStrings[_info->level] : levelStrings[kLevelFatal],
                           tag,
                           location[0] != '\0' ? location : "");

        assert(0 <= ret);
        _log.Length(_log.Pos() + ret, _log.Length() + ret);

        assert((unsigned int)_log.Pos() == _log.Length());
    }

    if (NULL != _logbody) {
        // in android 64bit, in strnlen memchr,  const unsigned char*  end = p + n;  > 4G!!!!! in stack array

        size_t bodylen =  _log.MaxLength() - _log.Length() > 130 ? _log.MaxLength() - _log.Length() - 130 : 0;
        bodylen = bodylen > 0xFFFFU ? 0xFFFFU : bodylen;
        bodylen = strnlen(_logbody, bodylen);
        bodylen = bodylen > 0xFFFFU ? 0xFFFFU : bodylen;
        
        // 处理多行日志（异常堆栈）：为后续行添加缩进，提高可读性
        const char* body = _logbody;
        size_t remaining = bodylen;
        bool isFirstLine = true;
        
        while (remaining > 0) {
            const char* lineStart = body;
            const char* lineEnd = (const char*)memchr(body, '\n', remaining);
            size_t lineLen = lineEnd ? (lineEnd - body) : remaining;
            
            if (lineLen > 0) {
                if (!isFirstLine && lineLen > 0) {
                    // 为多行日志的后续行添加缩进（4个空格），提高可读性
                    _log.Write("    ", 4);
                }
                _log.Write(lineStart, lineLen);
                isFirstLine = false;
            }
            
            if (lineEnd) {
                _log.Write("\n", 1);
                body = lineEnd + 1;
                remaining -= (lineLen + 1);
            } else {
                remaining = 0;
            }
        }
    } else {
        _log.Write("error!! NULL==_logbody");
    }

    char nextline = '\n';

    if (*((char*)_log.PosPtr() - 1) != nextline) _log.Write(&nextline, 1);
}

