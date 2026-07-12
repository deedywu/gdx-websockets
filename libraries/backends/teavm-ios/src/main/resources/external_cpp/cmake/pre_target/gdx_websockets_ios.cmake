if(APPLE)
  include_directories("${CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/websockets")
  set(TEAVM_WEBSOCKETS_IOS_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/c/external_cpp/app_include/ios/teavm_websocket_ios.m")
  if(EXISTS "${TEAVM_WEBSOCKETS_IOS_SOURCE}")
    list(APPEND SOURCES "${TEAVM_WEBSOCKETS_IOS_SOURCE}")
    set_source_files_properties("${TEAVM_WEBSOCKETS_IOS_SOURCE}" PROPERTIES
        LANGUAGE OBJC
        COMPILE_FLAGS "-fobjc-arc")
  endif()
endif()
