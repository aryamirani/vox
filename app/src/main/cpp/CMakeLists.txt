cmake_minimum_required(VERSION 3.22.1)
project("vox_snpe")

set(VOX_SNPE_ROOT "${CMAKE_SOURCE_DIR}/../../../../snpe-sdk")

add_library(vox_snpe SHARED
    snpe_jni.cpp
    snpe_runtime_stub.cpp
)

find_library(log-lib log)

target_include_directories(vox_snpe PRIVATE
    ${CMAKE_SOURCE_DIR}
)

# Uncomment when SNPE SDK is installed at project root:
# include_directories(${VOX_SNPE_ROOT}/include/SNPE)
# link_directories(${VOX_SNPE_ROOT}/lib/aarch64-android)
# target_link_libraries(vox_snpe SNPE ${log-lib})

target_link_libraries(vox_snpe ${log-lib})
