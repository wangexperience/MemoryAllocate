//
// Created by wangxinpeng on 2023/8/16.
//

#ifndef MEMORYALLOCATE_LOGUTIL_H
#define MEMORYALLOCATE_LOGUTIL_H

#endif //MEMORYALLOCATE_LOGUTIL_H

#include <android/log.h>
#define LOG_TAG    "wxp-log"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
