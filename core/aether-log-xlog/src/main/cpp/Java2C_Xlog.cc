// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.

#include <jni.h>
#include <pthread.h>
#include <android/log.h>

#include <vector>
#include <string>

#include "aether/common/xlogger/xlogger.h"
#include "aether/common/util/scoped_jstring.h"
#include "aether/common/util/var_cache.h"
#include "aether/common/util/scope_jenv.h"
#include "aether/common/util/comm_function.h"

#include "aether/log/appender.h"
#include "aether/log/xlogger_interface.h"
#include "aether/log/xlog_config.h"
#include "aether/log/xlogger_appender.h"
#include "aether/common/xlogger/xlogger_category.h"

#define LONGTHREADID2INT(a) ((a >> 32)^((a & 0xFFFF)))
DEFINE_FIND_CLASS(KXlog, "com/kernelflux/aether/log/xlog/Xlog")

// Global JNI environment key for thread-local storage
pthread_key_t g_env_key;

static void __DetachCurrentThread(void *a) {
    if (NULL != VarCache::Singleton()->GetJvm()) {
        VarCache::Singleton()->GetJvm()->DetachCurrentThread();
    }
}

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    if (0 != pthread_key_create(&g_env_key, __DetachCurrentThread)) {
        __android_log_print(ANDROID_LOG_ERROR, "AetherXlog", "create g_env_key fail");
        return (-1);
    }

    ScopeJEnv jenv(jvm);
    VarCache::Singleton()->SetJvm(jvm);

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    VarCache::Release();
}

DEFINE_FIND_STATIC_METHOD(KXlog_appenderOpenWithMultipathWithLevel, KXlog, "appenderOpen",
                          "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Z)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_appenderOpen
        (JNIEnv *env, jclass, jint level, jint mode, jstring _cache_dir, jstring _log_dir,
         jstring _nameprefix, jint _cache_log_days, jstring _pubkey, jboolean _is_compress) {
    if (NULL == _log_dir || NULL == _nameprefix) {
        return;
    }

    std::string cache_dir;
    if (NULL != _cache_dir) {
        ScopedJstring cache_dir_jstr(env, _cache_dir);
        cache_dir = cache_dir_jstr.GetChar();
    }

    const char *pubkey = NULL;
    ScopedJstring jstr_pubkey(env, _pubkey);
    if (NULL != _pubkey) {
        pubkey = jstr_pubkey.GetChar();
    }

    ScopedJstring log_dir_jstr(env, _log_dir);
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    appender_open_with_cache((TAppenderMode) mode, cache_dir.c_str(), log_dir_jstr.GetChar(),
                             nameprefix_jstr.GetChar(), _cache_log_days, pubkey,
                             (bool) _is_compress);
    xlogger_SetLevel((TLogLevel) level);

}

JNIEXPORT void JNICALL
Java_com_kernelflux_aether_log_xlog_Xlog_appenderCloseNative(JNIEnv *env, jclass clazz) {
    appender_close();
}

DEFINE_FIND_STATIC_METHOD(KXlog_flushAll, KXlog, "flushAll",
                          "(Z)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_flushAll
        (JNIEnv *env, jclass, jboolean _is_sync) {
    aether::xlog::FlushAll((bool)_is_sync);
}

DEFINE_FIND_STATIC_METHOD(KXlog_flushModule, KXlog, "flushModule",
                          "(Ljava/lang/String;Z)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_flushModule
        (JNIEnv *env, jclass, jstring _nameprefix, jboolean _is_sync) {
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    aether::xlog::FlushModule(nameprefix_jstr.GetChar(), (bool)_is_sync);
}

DEFINE_FIND_STATIC_METHOD(KXlog_getLogFiles, KXlog, "getLogFiles",
                          "(Ljava/lang/String;)[Ljava/lang/String;")
JNIEXPORT jobjectArray JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_getLogFiles
        (JNIEnv *env, jclass, jstring _nameprefix) {
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    aether::comm::XloggerCategory* category = aether::xlog::GetXloggerInstance(nameprefix_jstr.GetChar());
    
    if (category == nullptr) {
        // Return empty array
        jclass stringClass = env->FindClass("java/lang/String");
        return env->NewObjectArray(0, stringClass, nullptr);
    }
    
    aether::xlog::XloggerAppender* appender = reinterpret_cast<aether::xlog::XloggerAppender*>(category->GetAppender());
    if (appender == nullptr) {
        jclass stringClass = env->FindClass("java/lang/String");
        return env->NewObjectArray(0, stringClass, nullptr);
    }
    
    std::vector<std::string> filepaths;
    appender->GetLogFilePaths(filepaths);
    
    jclass stringClass = env->FindClass("java/lang/String");
    if (stringClass == nullptr) {
        return nullptr;
    }
    
    jobjectArray result = env->NewObjectArray((jsize)filepaths.size(), stringClass, nullptr);
    if (result == nullptr) {
        return nullptr;
    }
    
    for (size_t i = 0; i < filepaths.size(); ++i) {
        jstring jstr = env->NewStringUTF(filepaths[i].c_str());
        if (jstr != nullptr) {
            env->SetObjectArrayElement(result, (jsize)i, jstr);
            env->DeleteLocalRef(jstr);
        }
    }
    
    return result;
}

// Helper function to convert LogFileInfo to Java object
static jobject CreateLogFileInfoObject(JNIEnv* env, const aether::xlog::XloggerAppender::LogFileInfo& info, const char* moduleName) {
    jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
    if (logFileInfoClass == nullptr) {
        return nullptr;
    }
    
    jmethodID constructor = env->GetMethodID(logFileInfoClass, "<init>", 
        "(Ljava/lang/String;JJLjava/lang/String;Lcom/kernelflux/aether/log/api/LogFileInfo$FileType;)V");
    if (constructor == nullptr) {
        return nullptr;
    }
    
    jstring path = env->NewStringUTF(info.path.c_str());
    jstring module = env->NewStringUTF(moduleName);
    
    // Get FileType enum
    jclass fileTypeClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo$FileType");
    if (fileTypeClass == nullptr) {
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(module);
        return nullptr;
    }
    
    const char* enumName = info.is_cache ? "CACHE_DIR" : "LOG_DIR";
    jfieldID fileTypeField = env->GetStaticFieldID(fileTypeClass, enumName, 
        "Lcom/kernelflux/aether/log/api/LogFileInfo$FileType;");
    if (fileTypeField == nullptr) {
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(module);
        return nullptr;
    }
    
    jobject fileType = env->GetStaticObjectField(fileTypeClass, fileTypeField);
    if (fileType == nullptr) {
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(module);
        return nullptr;
    }
    
    // Convert time_t (seconds) to milliseconds
    jlong lastModified = (jlong)(info.mtime * 1000);
    
    jobject result = env->NewObject(logFileInfoClass, constructor, 
        path, (jlong)info.size, lastModified, module, fileType);
    
    env->DeleteLocalRef(path);
    env->DeleteLocalRef(module);
    env->DeleteLocalRef(fileType);
    
    return result;
}

DEFINE_FIND_STATIC_METHOD(KXlog_getLogFileInfos, KXlog, "getLogFileInfos",
                          "(Ljava/lang/String;)[Lcom/kernelflux/aether/log/api/LogFileInfo;")
JNIEXPORT jobjectArray JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_getLogFileInfos
        (JNIEnv *env, jclass, jstring _nameprefix) {
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    aether::comm::XloggerCategory* category = aether::xlog::GetXloggerInstance(nameprefix_jstr.GetChar());
    
    if (category == nullptr) {
        jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
        return env->NewObjectArray(0, logFileInfoClass, nullptr);
    }
    
    aether::xlog::XloggerAppender* appender = reinterpret_cast<aether::xlog::XloggerAppender*>(category->GetAppender());
    if (appender == nullptr) {
        jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
        return env->NewObjectArray(0, logFileInfoClass, nullptr);
    }
    
    std::vector<aether::xlog::XloggerAppender::LogFileInfo> fileinfos;
    appender->GetLogFileInfos(fileinfos);
    
    jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
    if (logFileInfoClass == nullptr) {
        return nullptr;
    }
    
    jobjectArray result = env->NewObjectArray((jsize)fileinfos.size(), logFileInfoClass, nullptr);
    if (result == nullptr) {
        return nullptr;
    }
    
    const char* moduleName = nameprefix_jstr.GetChar();
    for (size_t i = 0; i < fileinfos.size(); ++i) {
        jobject infoObj = CreateLogFileInfoObject(env, fileinfos[i], moduleName);
        if (infoObj != nullptr) {
            env->SetObjectArrayElement(result, (jsize)i, infoObj);
            env->DeleteLocalRef(infoObj);
        }
    }
    
    return result;
}

DEFINE_FIND_STATIC_METHOD(KXlog_getLogFileInfosByDays, KXlog, "getLogFileInfosByDays",
                          "(Ljava/lang/String;I)[Lcom/kernelflux/aether/log/api/LogFileInfo;")
JNIEXPORT jobjectArray JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_getLogFileInfosByDays
        (JNIEnv *env, jclass, jstring _nameprefix, jint _days_ago) {
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    aether::comm::XloggerCategory* category = aether::xlog::GetXloggerInstance(nameprefix_jstr.GetChar());
    
    if (category == nullptr) {
        jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
        return env->NewObjectArray(0, logFileInfoClass, nullptr);
    }
    
    aether::xlog::XloggerAppender* appender = reinterpret_cast<aether::xlog::XloggerAppender*>(category->GetAppender());
    if (appender == nullptr) {
        jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
        return env->NewObjectArray(0, logFileInfoClass, nullptr);
    }
    
    std::vector<aether::xlog::XloggerAppender::LogFileInfo> fileinfos;
    appender->GetLogFileInfosByDays(fileinfos, (int)_days_ago);
    
    jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
    if (logFileInfoClass == nullptr) {
        return nullptr;
    }
    
    jobjectArray result = env->NewObjectArray((jsize)fileinfos.size(), logFileInfoClass, nullptr);
    if (result == nullptr) {
        return nullptr;
    }
    
    const char* moduleName = nameprefix_jstr.GetChar();
    for (size_t i = 0; i < fileinfos.size(); ++i) {
        jobject infoObj = CreateLogFileInfoObject(env, fileinfos[i], moduleName);
        if (infoObj != nullptr) {
            env->SetObjectArrayElement(result, (jsize)i, infoObj);
            env->DeleteLocalRef(infoObj);
        }
    }
    
    return result;
}

DEFINE_FIND_STATIC_METHOD(KXlog_getLogFileInfosByTimeRange, KXlog, "getLogFileInfosByTimeRange",
                          "(Ljava/lang/String;JJ)[Lcom/kernelflux/aether/log/api/LogFileInfo;")
JNIEXPORT jobjectArray JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_getLogFileInfosByTimeRange
        (JNIEnv *env, jclass, jstring _nameprefix, jlong _start_time, jlong _end_time) {
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    aether::comm::XloggerCategory* category = aether::xlog::GetXloggerInstance(nameprefix_jstr.GetChar());
    
    if (category == nullptr) {
        jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
        return env->NewObjectArray(0, logFileInfoClass, nullptr);
    }
    
    aether::xlog::XloggerAppender* appender = reinterpret_cast<aether::xlog::XloggerAppender*>(category->GetAppender());
    if (appender == nullptr) {
        jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
        return env->NewObjectArray(0, logFileInfoClass, nullptr);
    }
    
    // Convert milliseconds to seconds
    time_t start_time = (time_t)(_start_time / 1000);
    time_t end_time = (time_t)(_end_time / 1000);
    
    std::vector<aether::xlog::XloggerAppender::LogFileInfo> fileinfos;
    appender->GetLogFileInfosByTimeRange(fileinfos, start_time, end_time);
    
    jclass logFileInfoClass = env->FindClass("com/kernelflux/aether/log/api/LogFileInfo");
    if (logFileInfoClass == nullptr) {
        return nullptr;
    }
    
    jobjectArray result = env->NewObjectArray((jsize)fileinfos.size(), logFileInfoClass, nullptr);
    if (result == nullptr) {
        return nullptr;
    }
    
    const char* moduleName = nameprefix_jstr.GetChar();
    for (size_t i = 0; i < fileinfos.size(); ++i) {
        jobject infoObj = CreateLogFileInfoObject(env, fileinfos[i], moduleName);
        if (infoObj != nullptr) {
            env->SetObjectArrayElement(result, (jsize)i, infoObj);
            env->DeleteLocalRef(infoObj);
        }
    }
    
    return result;
}

DEFINE_FIND_STATIC_METHOD(KXlog_clearFileCache, KXlog, "clearFileCache",
                          "(Ljava/lang/String;)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_clearFileCache
        (JNIEnv *env, jclass, jstring _nameprefix) {
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    aether::xlog::ClearFileCache(nameprefix_jstr.GetChar());
}

DEFINE_FIND_STATIC_METHOD(KXlog_clearAllFileCache, KXlog, "clearAllFileCache", "()V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_clearAllFileCache
        (JNIEnv *env, jclass) {
    aether::xlog::ClearAllFileCache();
}


DEFINE_FIND_STATIC_METHOD(KXlog_logWrite2, KXlog, "logWrite2",
                          "(JILjava/lang/String;Ljava/lang/String;Ljava/lang/String;IIJJLjava/lang/String;)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_logWrite2
        (JNIEnv *env, jclass, jlong _instance_ptr, int _level, jstring _tag, jstring _filename,
         jstring _funcname, jint _line, jint _pid, jlong _tid, jlong _maintid, jstring _log) {

    uintptr_t instance_ptr = (uintptr_t)_instance_ptr;
    
    if (!aether::xlog::IsEnabledFor(instance_ptr, (TLogLevel) _level)) {
        return;
    }

    XLoggerInfo xlog_info;
    gettimeofday(&xlog_info.timeval, NULL);
    xlog_info.level = (TLogLevel) _level;
    xlog_info.line = (int) _line;
    xlog_info.pid = (int) _pid;
    xlog_info.tid = LONGTHREADID2INT(_tid);
    xlog_info.maintid = LONGTHREADID2INT(_maintid);

    const char *tag_cstr = NULL;
    const char *filename_cstr = NULL;
    const char *funcname_cstr = NULL;
    const char *log_cstr = NULL;

    if (NULL != _tag) {
        tag_cstr = env->GetStringUTFChars(_tag, NULL);
    }

    if (NULL != _filename) {
        filename_cstr = env->GetStringUTFChars(_filename, NULL);
    }

    if (NULL != _funcname) {
        funcname_cstr = env->GetStringUTFChars(_funcname, NULL);
    }

    if (NULL != _log) {
        log_cstr = env->GetStringUTFChars(_log, NULL);
    }

    xlog_info.tag = NULL == tag_cstr ? "" : tag_cstr;
    xlog_info.filename = NULL == filename_cstr ? "" : filename_cstr;
    xlog_info.func_name = NULL == funcname_cstr ? "" : funcname_cstr;

    aether::xlog::XloggerWrite(instance_ptr, &xlog_info, NULL == log_cstr ? "NULL == log" : log_cstr);

    if (NULL != _tag) {
        env->ReleaseStringUTFChars(_tag, tag_cstr);
    }

    if (NULL != _filename) {
        env->ReleaseStringUTFChars(_filename, filename_cstr);
    }

    if (NULL != _funcname) {
        env->ReleaseStringUTFChars(_funcname, funcname_cstr);
    }

    if (NULL != _log) {
        env->ReleaseStringUTFChars(_log, log_cstr);
    }
}

JNIEXPORT jint JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_getLogLevelNative
        (JNIEnv *, jclass clazz) {
    return xlogger_Level();
}

JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_setLogLevel
        (JNIEnv *, jclass, jint _log_level) {
    xlogger_SetLevel((TLogLevel) _log_level);
}

DEFINE_FIND_STATIC_METHOD(KXlog_setAppenderMode, KXlog, "setAppenderMode", "(I)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_setAppenderMode
        (JNIEnv *, jclass, jint _mode) {
    appender_setmode((TAppenderMode) _mode);
}

DEFINE_FIND_STATIC_METHOD(KXlog_setConsoleLogOpen, KXlog, "setConsoleLogOpen", "(Z)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_setConsoleLogOpen
        (JNIEnv *env, jclass, jboolean _is_open) {
    appender_set_console_log((bool) _is_open);
}

DEFINE_FIND_STATIC_METHOD(KXlog_setMaxFileSize, KXlog, "setMaxFileSize", "(J)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_setMaxFileSize
        (JNIEnv *env, jclass, jlong _maxSize) {
    appender_set_max_file_size(_maxSize);
}

DEFINE_FIND_STATIC_METHOD(KXlog_setMaxAliveTime, KXlog, "setMaxAliveTime", "(J)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_setMaxAliveTime
        (JNIEnv *env, jclass, jlong _maxTime) {
    appender_set_max_alive_duration(_maxTime);
}

DEFINE_FIND_STATIC_METHOD(KXlog_setCustomHeaderInfo, KXlog, "setCustomHeaderInfo",
                          "(Ljava/lang/String;)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_setCustomHeaderInfo
        (JNIEnv *env, jclass, jstring _headerInfo) {
    if (NULL == _headerInfo) {
        appender_setExtraMSg(NULL, 0);
        return;
    }

    ScopedJstring jstr_header(env, _headerInfo);
    const char *header_cstr = jstr_header.GetChar();
    if (NULL != header_cstr) {
        size_t len = strlen(header_cstr);
        appender_setExtraMSg(header_cstr, (unsigned int) len);
    }
}

DEFINE_FIND_STATIC_METHOD(KXlog_newXlogInstance, KXlog, "newXlogInstance",
                          "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Z)J")
JNIEXPORT jlong JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_newXlogInstance
        (JNIEnv *env, jclass, jint _level, jint _mode, jstring _cache_dir, jstring _log_dir,
         jstring _nameprefix, jint _cache_days, jstring _pubkey, jboolean _is_compress) {
    if (NULL == _log_dir || NULL == _nameprefix) {
        return 0;
    }

    std::string cache_dir;
    if (NULL != _cache_dir) {
        ScopedJstring cache_dir_jstr(env, _cache_dir);
        cache_dir = cache_dir_jstr.GetChar();
    }

    const char *pubkey = NULL;
    ScopedJstring jstr_pubkey(env, _pubkey);
    if (NULL != _pubkey) {
        pubkey = jstr_pubkey.GetChar();
    }

    ScopedJstring log_dir_jstr(env, _log_dir);
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    
    aether::xlog::XLogConfig config;
    config.mode_ = (TAppenderMode)_mode;
    config.logdir_ = log_dir_jstr.GetChar();
    config.nameprefix_ = nameprefix_jstr.GetChar();
    config.pub_key_ = pubkey ? std::string(pubkey) : std::string("");
    config.is_compress_ = (bool)_is_compress;
    config.cachedir_ = cache_dir;
    config.cache_days_ = _cache_days;
    
    aether::comm::XloggerCategory* category = aether::xlog::NewXloggerInstance(config, (TLogLevel)_level);
    if (nullptr == category) {
        return 0;
    }
    return reinterpret_cast<jlong>(category);
}

DEFINE_FIND_STATIC_METHOD(KXlog_getXlogInstance, KXlog, "getXlogInstance",
                          "(Ljava/lang/String;)J")
JNIEXPORT jlong JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_getXlogInstance
        (JNIEnv *env, jclass, jstring _nameprefix) {
    if (NULL == _nameprefix) {
        return 0;
    }
    
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    aether::comm::XloggerCategory* category = aether::xlog::GetXloggerInstance(nameprefix_jstr.GetChar());
    if (nullptr == category) {
        return 0;
    }
    return reinterpret_cast<jlong>(category);
}

DEFINE_FIND_STATIC_METHOD(KXlog_releaseXlogInstance, KXlog, "releaseXlogInstance",
                          "(Ljava/lang/String;)V")
JNIEXPORT void JNICALL Java_com_kernelflux_aether_log_xlog_Xlog_releaseXlogInstance
        (JNIEnv *env, jclass, jstring _nameprefix) {
    if (NULL == _nameprefix) {
        return;
    }
    
    ScopedJstring nameprefix_jstr(env, _nameprefix);
    aether::xlog::ReleaseXloggerInstance(nameprefix_jstr.GetChar());
}
}

void ExportXlog() {}

