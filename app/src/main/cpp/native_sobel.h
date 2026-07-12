#ifndef NATIVE_SOBEL_H
#define NATIVE_SOBEL_H

#include <jni.h>

jintArray native_process_sobel(
    JNIEnv* env,
    jintArray imageData,
    jint width,
    jint height
);

#endif
