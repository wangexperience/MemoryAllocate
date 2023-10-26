#include <jni.h>
#include <string>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <iostream>
#include "log/logutil.h"

using namespace std;

extern "C" JNIEXPORT jstring JNICALL
Java_com_wxp_memoryallocate_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_wxp_memoryallocate_MainActivity_allocMemoryFromJNI(JNIEnv *env, jobject thiz) {

    // 分配堆内存空间
    // 单位：            byte /  KB   / MB   / GB   / TB
    int allocate_size = 100 * 1024 * 1024;

    void * pInt = NULL;

    // 分配内存
    try {
        pInt = malloc(allocate_size);
        memset(pInt,1, allocate_size);
        if (pInt != NULL) {
            LOGD("native分配堆内存成功");
        } else {
            LOGD("native分配堆内存失败");
        }
    } catch (std::exception) {
        LOGD("分配堆内存时抛出异常！std::exception");
    }

//    分配内存后不进行回收
//    free(pInt);

    return reinterpret_cast<jlong>(pInt);
}

extern "C" JNIEXPORT void JNICALL
Java_com_wxp_memoryallocate_MainActivity_freeMemoryFromJNI(JNIEnv *env, jobject thiz, jlong ptr) {
    if(ptr != NULL) {
        free(reinterpret_cast<void*>(ptr));
    }
}