cmake_minimum_required(VERSION 3.22.1)

project("imagebenchmark")

add_library(
        native-lib
        SHARED
        native-lib.c)

find_library(
        log-lib
        log)

target_link_libraries(
        native-lib
        ${log-lib})