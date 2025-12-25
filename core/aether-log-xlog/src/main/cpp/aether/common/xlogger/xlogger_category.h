// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.

#ifndef XLOGGER_CATEGORY_H_
#define XLOGGER_CATEGORY_H_

#include <functional>
#include "xloggerbase.h"
#include "../thread/thread.h"

namespace aether {
namespace comm {

class XloggerCategory {
 public:
    static XloggerCategory* NewInstance(uintptr_t _appender,
                                        std::function<void(const XLoggerInfo* _info, const char* _log)> _appender_func);
    static void DelayRelease(XloggerCategory* _category);

 private:
    XloggerCategory(uintptr_t _appender,
                    std::function<void(const XLoggerInfo* _info, const char* _log)> _appender_func);
    static void __Release(XloggerCategory* _category);

 public:
    intptr_t GetAppender();
    TLogLevel GetLevel();
    void SetLevel(TLogLevel _level);
    bool IsEnabledFor(TLogLevel _level);
    void VPrint(const XLoggerInfo* _info, const char* _format, va_list _list);
    void Print(const XLoggerInfo* _info, const char* _format, ...);
    void Write(const XLoggerInfo* _info, const char* _log);

 private:
    void __WriteImpl(const XLoggerInfo* _info, const char* _log);

 private:
    TLogLevel level_ = kLevelNone;
    uintptr_t appender_ = 0;
    std::function<void(const XLoggerInfo* _info, const char* _log)> appender_func_ = nullptr;
};

}  // namespace comm
}  // namespace aether

#endif  // XLOGGER_CATEGORY_H_

