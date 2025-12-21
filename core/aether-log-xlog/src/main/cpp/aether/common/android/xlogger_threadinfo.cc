// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.

#include "xlogger/xloggerbase.h"
#include <unistd.h>
#include <sys/syscall.h>
#include <pthread.h>

#ifdef __cplusplus
extern "C" {
#endif

intmax_t xlogger_pid() {
    static intmax_t pid = getpid();
    return pid;
}

intmax_t xlogger_tid() {
#ifdef __NR_gettid
    return (intmax_t)syscall(__NR_gettid);
#else
    return (intmax_t)syscall(SYS_gettid);
#endif
}

intmax_t xlogger_maintid() {
    static intmax_t maintid = getpid();
    return maintid;
}

#ifdef __cplusplus
}
#endif

