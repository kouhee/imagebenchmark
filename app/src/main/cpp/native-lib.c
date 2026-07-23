#include <jni.h>

#include "native_grayscale.h"
#include "native_sobel.h"
#include "native_thread_pool.h"

JNIEXPORT jintArray JNICALL
Java_com_kouhee_imagebenchmark_data_processor_NativeKotlinNaiveGrayScaleProcessor_convertToGrayScale(
    JNIEnv* env, jobject thiz, jintArray imageData, jint width, jint height
) {
    (void)thiz;
    return native_process_grayscale(env, imageData, width, height, 0);
}

JNIEXPORT jintArray JNICALL
Java_com_kouhee_imagebenchmark_data_processor_NativeInterpolatedGrayScaleProcessor_convertToGrayScale(
    JNIEnv* env, jobject thiz, jintArray imageData, jint width, jint height
) {
    (void)thiz;
    return native_process_grayscale(env, imageData, width, height, 1);
}

JNIEXPORT jintArray JNICALL
Java_com_kouhee_imagebenchmark_data_processor_NativeSobelProcessor_detectEdgesSobel(
    JNIEnv* env, jobject thiz, jintArray imageData, jint width, jint height
) {
    (void)thiz;
    return native_process_sobel(env, imageData, width, height);
}

JNIEXPORT jstring JNICALL
Java_com_kouhee_imagebenchmark_MainActivity_stringFromJNI(JNIEnv* env, jobject thiz) {
    (void)thiz;
    return (*env)->NewStringUTF(env, "Hello from Optimized Native Lib!");
}

JNIEXPORT void JNICALL
Java_com_kouhee_imagebenchmark_presentation_viewmodel_CameraPreviewViewModel_nativeShutdownThreadPool(
    JNIEnv* env, jobject thiz
) {
    (void)env;
    (void)thiz;
    native_thread_pool_shutdown();
}
