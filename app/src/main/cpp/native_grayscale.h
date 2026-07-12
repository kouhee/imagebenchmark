#ifndef NATIVE_GRAYSCALE_H
#define NATIVE_GRAYSCALE_H

#include <jni.h>

jintArray native_process_grayscale(
    JNIEnv* env,
    jintArray imageData,
    jint width,
    jint height,
    int interpolated
);

#endif
