#include <jni.h>
#include <android/log.h>
#include <unwind.h>
#include <dlfcn.h>
#include <stdio.h>

#define LOG_TAG "NativeLib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

struct BacktraceState {
    void** current;
    void** end;
};

static _Unwind_Reason_Code unwind_callback(struct _Unwind_Context* context, void* arg) {
    struct BacktraceState* state = (struct BacktraceState*)arg;
    uintptr_t pc = _Unwind_GetIP(context);
    if (pc) {
        if (state->current == state->end) {
            return _URC_END_OF_STACK;
        } else {
            *state->current++ = (void*)pc;
        }
    }
    return _URC_NO_REASON;
}

void print_native_backtrace() {
    const size_t max_frames = 30;
    void* buffer[max_frames];
    struct BacktraceState state = {buffer, buffer + max_frames};

    _Unwind_Backtrace(unwind_callback, &state);

    size_t count = state.current - buffer;
    LOGI("--- Native Backtrace (C side) ---");
    for (size_t i = 0; i < count; ++i) {
        const void* addr = buffer[i];
        const char* symbol = "";
        Dl_info info;
        if (dladdr(addr, &info) && info.dli_sname) {
            symbol = info.dli_sname;
        }
        LOGI("#%02zu pc %p %s %s", i, addr, info.dli_fname ? info.dli_fname : "", symbol);
    }
}

JNIEXPORT jstring JNICALL
Java_com_kouhee_imagebenchmark_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject thiz) {
    LOGI("stringFromJNI called from Kotlin!");
    print_native_backtrace();
    return (*env)->NewStringUTF(env, "Hello from C (NDK)!");
}

// ここに変換処理を書く
JNIEXPORT jintArray JNICALL Java_com_kouhee_imagebenchmark_data_processor_NativeKotlinNaiveGrayScaleProcessor_convertToGrayScale(
        JNIEnv* env,
        jobject thiz,
        jintArray imageData,
        jint width,
        jint height) {
    LOGI("convertToGrayScale called from Kotlin!");
    LOGI("width: %d, height: %d", width, height);

    jint *pixels = (*env)->GetIntArrayElements(env, imageData, NULL);
    if (pixels == NULL) {
        return NULL;
    }
    LOGI("imageData[0]: %x", pixels[0]);
    (*env)->ReleaseIntArrayElements(env, imageData, pixels, JNI_ABORT);
    jintArray result = (*env)->NewIntArray(env, width * height);
    if (result == NULL) {
        return NULL; // Out of memory error thrown
    }
    return result;
}