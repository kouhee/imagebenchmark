cmake_minimum_required(VERSION 3.22.1)

project("imagebenchmark")

add_library(
        native-lib
        SHARED
        native-lib.c
        native_grayscale.c
        native_sobel.c
        native_thread_pool.c)

# Enable NEON for all ARM architectures
if(CMAKE_ANDROID_ARCH_ABI MATCHES "^armeabi")
    # ARM 32-bit
    target_compile_options(native-lib PRIVATE -mfloat-abi=softfp -mfpu=neon)
    target_compile_definitions(native-lib PRIVATE __ARM_NEON__)
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    # ARM 64-bit - ASIMD/NEON always available
    target_compile_definitions(native-lib PRIVATE __ARM_NEON__)
endif()

find_library(
        log-lib
        log)

find_library(
        android-lib
        android)

target_link_libraries(
        native-lib
        ${log-lib}
        ${android-lib})