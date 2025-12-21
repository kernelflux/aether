cmake_minimum_required(VERSION 3.18.1)

project(aether-boost)

# Include directories - boost headers are in the current directory
include_directories(.)
include_directories(..)

# Boost configuration
add_definitions(-DBOOST_NO_EXCEPTIONS)

# Source files for required Boost components
# Use absolute paths relative to this CMakeLists.txt
set(BOOST_SRC_FILES
    # Filesystem
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/filesystem/src/codecvt_error_category.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/filesystem/src/operations.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/filesystem/src/path.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/filesystem/src/path_traits.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/filesystem/src/portability.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/filesystem/src/unique_path.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/filesystem/src/utf8_codecvt_facet.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/filesystem/src/windows_file_codecvt.cpp
    # Iostreams (for mapped_file)
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/iostreams/src/file_descriptor.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/iostreams/src/mapped_file.cpp
    # System (required by filesystem and iostreams)
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/system/src/error_code.cpp
    # Smart pointer (required by other components)
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/smart_ptr/src/sp_collector.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/smart_ptr/src/sp_debug_hooks.cpp
    # Atomic (required by other components)
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/atomic/src/lockpool.cpp
    # Exception (required by other components)
    ${CMAKE_CURRENT_SOURCE_DIR}/libs/exception/src/clone_current_exception_non_intrusive.cpp
)

# Android-specific sources
if(ANDROID)
    # Context and coroutine for Android (required by iostreams)
    file(GLOB ANDROID_CONTEXT_SRC
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/*.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/posix/*.cpp
    )
    file(GLOB ANDROID_COROUTINE_SRC
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/coroutine/src/*.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/coroutine/src/detail/*.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/coroutine/src/posix/*.cpp
    )
    
    list(APPEND BOOST_SRC_FILES ${ANDROID_CONTEXT_SRC} ${ANDROID_COROUTINE_SRC})
    
    # Enable assembly language for context switching
    enable_language(ASM)
    
    # Architecture-specific assembly files
    if(ANDROID_ABI MATCHES "^armeabi(-v7a)?$")
        list(APPEND BOOST_SRC_FILES
            ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/asm/jump_arm_aapcs_elf_gas.S
            ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/asm/make_arm_aapcs_elf_gas.S
        )
    elseif(ANDROID_ABI STREQUAL "arm64-v8a")
        list(APPEND BOOST_SRC_FILES
            ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/asm/jump_arm64_aapcs_elf_gas.S
            ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/asm/make_arm64_aapcs_elf_gas.S
        )
    elseif(ANDROID_ABI STREQUAL "x86")
        list(APPEND BOOST_SRC_FILES
            ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/asm/jump_i386_sysv_elf_gas.S
            ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/asm/make_i386_sysv_elf_gas.S
        )
    elseif(ANDROID_ABI STREQUAL "x86_64")
        list(APPEND BOOST_SRC_FILES
            ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/asm/jump_x86_64_sysv_elf_gas.S
            ${CMAKE_CURRENT_SOURCE_DIR}/libs/context/src/asm/make_x86_64_sysv_elf_gas.S
        )
    endif()
endif()

# Create static library
add_library(${PROJECT_NAME} STATIC ${BOOST_SRC_FILES})

# Compiler flags
target_compile_options(${PROJECT_NAME} PRIVATE
    -Wall
    -Wextra
    -Wno-unused-parameter
    -fexceptions
    -frtti
    -fvisibility=hidden
    -DANDROID
    -D__ANDROID__
)

# C++ standard
set_target_properties(${PROJECT_NAME} PROPERTIES
    CXX_STANDARD 11
    CXX_STANDARD_REQUIRED ON
    CXX_VISIBILITY_PRESET hidden
)
