# SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
# SPDX-License-Identifier: MPL-2.0

cmake_minimum_required(VERSION 3.20)
project(pgm_bindings LANGUAGES C CXX)
find_package(LLVM 23.1.4 EXACT REQUIRED CONFIG)
find_package(Clang 23.1.4 EXACT REQUIRED CONFIG)
add_executable(pgm-extract Extract.cpp)
target_compile_features(pgm-extract PRIVATE cxx_std_17)
target_include_directories(pgm-extract SYSTEM PRIVATE ${LLVM_INCLUDE_DIRS} ${CLANG_INCLUDE_DIRS})
target_compile_definitions(pgm-extract PRIVATE ${LLVM_DEFINITIONS})
if(TARGET clang-cpp AND TARGET LLVM)
    target_link_libraries(pgm-extract PRIVATE clang-cpp LLVM)
else()
    target_link_libraries(pgm-extract PRIVATE clangTooling clangFrontend clangAST LLVMSupport)
endif()
