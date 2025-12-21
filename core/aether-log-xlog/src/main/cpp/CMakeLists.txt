cmake_minimum_required(VERSION 3.18.1)
project("aetherxlog")

# Aether xlog source is integrated into the project
set(AETHER_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/aether")
set(AETHER_LOG_DIR "${AETHER_SOURCE_DIR}/log")
set(AETHER_COMMON_DIR "${AETHER_SOURCE_DIR}/common")
set(BOOST_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/boost")
# Boost include path should be the parent directory so #include <boost/...> works
set(BOOST_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}")

# Add Boost subdirectory (builds boost static library)
add_subdirectory(boost aether-boost)

# Include directories (global, for all targets)
include_directories(
    ${AETHER_SOURCE_DIR}
    ${AETHER_LOG_DIR}
    ${AETHER_LOG_DIR}/export_include
    ${AETHER_LOG_DIR}/export_include/xlogger
    ${AETHER_COMMON_DIR}
    ${AETHER_COMMON_DIR}/xlogger
    ${AETHER_COMMON_DIR}/util
    ${AETHER_COMMON_DIR}/thread
    ${AETHER_COMMON_DIR}/assert
    ${AETHER_COMMON_DIR}/android
    ${BOOST_INCLUDE_DIR}  # Boost headers - parent directory so #include <boost/...> works
)

# Log source files
set(AETHER_LOG_SRC
    "${AETHER_LOG_DIR}/appender.cc"
    "${AETHER_LOG_DIR}/formater.cc"
    "${AETHER_LOG_DIR}/log_buffer.cc"
)

# Log crypt source (required by log_buffer)
set(AETHER_LOG_CRYPT_SRC
    "${AETHER_LOG_DIR}/crypt/log_crypt.cc"
    "${AETHER_LOG_DIR}/crypt/micro-ecc-master/uECC.c"
)

# Common xlogger source
set(AETHER_COMMON_XLOGGER_SRC
    "${AETHER_COMMON_DIR}/xlogger/xloggerbase.c"
    "${AETHER_COMMON_DIR}/xlogger/loginfo_extract.c"
)

# Common utility sources
set(AETHER_COMMON_UTIL_SRC
    "${AETHER_COMMON_DIR}/autobuffer.cc"
    "${AETHER_COMMON_DIR}/ptrbuffer.cc"
    "${AETHER_COMMON_DIR}/strutil.cc"
    "${AETHER_COMMON_DIR}/time_utils.c"
    "${AETHER_COMMON_DIR}/tickcount.cc"
    "${AETHER_COMMON_DIR}/mmap_util.cc"
    "${AETHER_COMMON_DIR}/assert/__assert.c"
    "${AETHER_COMMON_DIR}/android/xlogger_threadinfo.cc"
)

# JNI utility sources (required for JNI helpers)
file(GLOB AETHER_JNI_UTIL_SRC
    "${AETHER_COMMON_DIR}/util/*.cc"
)

# Our JNI files
set(OUR_JNI_SRC
    "${CMAKE_CURRENT_SOURCE_DIR}/Java2C_Xlog.cc"
    "${CMAKE_CURRENT_SOURCE_DIR}/ConsoleLog.cc"
)

# Boost exception handler (required when BOOST_NO_EXCEPTIONS is defined)
set(BOOST_EXCEPTION_SRC
    "${CMAKE_CURRENT_SOURCE_DIR}/boost_exception.cc"
)

# Collect all source files
set(ALL_SOURCES
    ${AETHER_LOG_SRC}
    ${AETHER_LOG_CRYPT_SRC}
    ${AETHER_COMMON_XLOGGER_SRC}
    ${AETHER_COMMON_UTIL_SRC}
    ${AETHER_JNI_UTIL_SRC}
    ${OUR_JNI_SRC}
    ${BOOST_EXCEPTION_SRC}
)

# Create shared library
add_library(aetherxlog SHARED ${ALL_SOURCES})

# Ensure Boost headers are accessible to aetherxlog target
target_include_directories(aetherxlog PRIVATE
    ${AETHER_SOURCE_DIR}
    ${AETHER_LOG_DIR}
    ${AETHER_LOG_DIR}/export_include
    ${AETHER_LOG_DIR}/export_include/xlogger
    ${AETHER_COMMON_DIR}
    ${AETHER_COMMON_DIR}/xlogger
    ${AETHER_COMMON_DIR}/util
    ${AETHER_COMMON_DIR}/thread
    ${AETHER_COMMON_DIR}/assert
    ${AETHER_COMMON_DIR}/android
)

# Boost headers must be added separately - parent directory so #include <boost/...> works
target_include_directories(aetherxlog PRIVATE ${BOOST_INCLUDE_DIR})

# Link libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(z-lib z)

# Link Boost static library (built from source)
target_link_libraries(aetherxlog
    ${log-lib}
    ${android-lib}
    ${z-lib}
    aether-boost  # Boost static library built from source
)

# Compiler flags
target_compile_options(aetherxlog PRIVATE
    -Wall
    -Wextra
    -Wno-unused-parameter
    -fexceptions
    -frtti
    -fvisibility=hidden
    -DANDROID
    -D__ANDROID__
)

# Generate build time
string(TIMESTAMP BUILD_TIME "%Y-%m-%d %H:%M:%S" UTC)

# Try to get git revision if available
find_package(Git QUIET)
if(GIT_FOUND)
    execute_process(
        COMMAND ${GIT_EXECUTABLE} rev-parse --short HEAD
        WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
        OUTPUT_VARIABLE GIT_REVISION
        OUTPUT_STRIP_TRAILING_WHITESPACE
        ERROR_QUIET
    )
else()
    set(GIT_REVISION "unknown")
endif()

target_compile_definitions(aetherxlog PRIVATE
    ANDROID
    __ANDROID__
    AETHER_BUILD_TIME="${BUILD_TIME}"
    AETHER_REVISION="${GIT_REVISION}"
    AETHER_PATH="aether"
    AETHER_URL=""
    AETHER_TAG=""
)

# C++ standard
set_target_properties(aetherxlog PROPERTIES
    CXX_STANDARD 11
    CXX_STANDARD_REQUIRED ON
    CXX_VISIBILITY_PRESET hidden
)

