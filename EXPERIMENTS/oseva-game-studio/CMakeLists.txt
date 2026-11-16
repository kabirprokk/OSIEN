cmake_minimum_required(VERSION 3.24)

project(OSEVAEngine VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(OGS_BUILD_TESTS "Build OGS engine tests" ON)

add_library(ogs_engine_core STATIC
    engine/core/application.cpp
    engine/core/fixed_clock.cpp
    engine/core/log.cpp
    engine/editor/browser_game.cpp
    engine/editor/document.cpp
    engine/editor/studio_shell.cpp
    engine/platform/platform.cpp
    engine/physics/physics_world.cpp
)

target_include_directories(ogs_engine_core PUBLIC engine)
target_compile_definitions(ogs_engine_core PUBLIC UNICODE _UNICODE NOMINMAX)

if(WIN32)
    target_sources(ogs_engine_core PRIVATE
        engine/platform/windows_platform.cpp
        engine/platform/ogs_icon.rc
    )
    target_link_libraries(ogs_engine_core PRIVATE user32 gdi32 gdiplus ole32 mfplat mfplay mfuuid shlwapi comdlg32 shell32)
elseif(UNIX)
    target_sources(ogs_engine_core PRIVATE engine/platform/linux_platform.cpp)
endif()

if(MSVC)
    target_compile_options(ogs_engine_core PRIVATE /W4 /permissive- /utf-8)
else()
    target_compile_options(ogs_engine_core PRIVATE -Wall -Wextra -Wpedantic)
endif()

add_executable(ogs_editor WIN32
    engine/editor/editor_main.cpp
)
target_link_libraries(ogs_editor PRIVATE ogs_engine_core)

file(MAKE_DIRECTORY "${CMAKE_BINARY_DIR}/assets/branding")
configure_file("${CMAKE_SOURCE_DIR}/OGS Logos/oseva game studio in-game intro .mp4"
               "${CMAKE_BINARY_DIR}/assets/branding/ogs_intro.mp4" COPYONLY)
configure_file("${CMAKE_SOURCE_DIR}/OGS Logos/2.png"
               "${CMAKE_BINARY_DIR}/assets/branding/ogs_logo_primary.png" COPYONLY)
configure_file("${CMAKE_SOURCE_DIR}/Assets/Game Assets Player/1.png"
               "${CMAKE_BINARY_DIR}/assets/player/grid_soul_01.png" COPYONLY)

if(OGS_BUILD_TESTS)
    enable_testing()
    add_executable(ogs_core_tests
        engine/tests/core_tests.cpp
    )
    target_link_libraries(ogs_core_tests PRIVATE ogs_engine_core)
    add_test(NAME ogs_core_tests COMMAND ogs_core_tests)
endif()

