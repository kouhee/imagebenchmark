#include "native_grayscale.h"

#include <android/log.h>
#include <inttypes.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "native_thread_pool.h"

#define LOG_TAG "NativeGray"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static const int OPTIMAL_THREAD_COUNT = 4;
static const int RGB565_TABLE_SIZE = 1 << 16;

static uint32_t* grayTable = NULL;
static pthread_mutex_t table_lock = PTHREAD_MUTEX_INITIALIZER;

typedef struct {
    uint32_t* pixels;
    int start;
    int end;
    int interpolated;
} GrayThreadWorkItem;

typedef struct {
    uint32_t* pixels;
    int totalPixels;
    int interpolated;
} GrayProcessContext;

static inline uint32_t toRgb565Index(uint32_t pixel) {
    return ((pixel >> 8) & 0xF800U) |
        ((pixel >> 5) & 0x07E0U) |
        ((pixel >> 3) & 0x001FU);
}

static inline void write4Same(uint32_t* dst, uint32_t value) {
    uint64_t pair = ((uint64_t)value << 32) | (uint64_t)value;
    memcpy(dst, &pair, sizeof(pair));
    memcpy(dst + 2, &pair, sizeof(pair));
}

static void initGrayTable() {
    if (grayTable != NULL) {
        return;
    }

    pthread_mutex_lock(&table_lock);
    if (grayTable == NULL) {
        LOGI("Initializing grayscale table (RGB565, 256KB)...");
        struct timespec start;
        struct timespec end;
        clock_gettime(CLOCK_MONOTONIC, &start);

        grayTable = (uint32_t*)malloc((size_t)RGB565_TABLE_SIZE * sizeof(uint32_t));
        if (grayTable == NULL) {
            LOGE("Failed to allocate grayscale table");
            pthread_mutex_unlock(&table_lock);
            return;
        }

        for (int rgb565 = 0; rgb565 < RGB565_TABLE_SIZE; rgb565++) {
            int r = ((rgb565 >> 11) & 0x1F) * 255 / 31;
            int g = ((rgb565 >> 5) & 0x3F) * 255 / 63;
            int b = (rgb565 & 0x1F) * 255 / 31;
            uint32_t gray = ((r + g + b) * 0x5555) >> 16;
            grayTable[rgb565] = 0xFF000000U | (gray << 16) | (gray << 8) | gray;
        }

        clock_gettime(CLOCK_MONOTONIC, &end);
        uint64_t elapsed = (end.tv_sec - start.tv_sec) * 1000ULL +
            (uint64_t)(end.tv_nsec - start.tv_nsec) / 1000000ULL;
        LOGI("Grayscale table initialized in %" PRIu64 " ms", elapsed);
    }
    pthread_mutex_unlock(&table_lock);
}

static void processTableRange(uint32_t* pixels, int start, int end) {
    uint32_t* table = grayTable;
    int i = start;
    for (; i + 7 < end; i += 8) {
        pixels[i] = table[toRgb565Index(pixels[i])];
        pixels[i + 1] = table[toRgb565Index(pixels[i + 1])];
        pixels[i + 2] = table[toRgb565Index(pixels[i + 2])];
        pixels[i + 3] = table[toRgb565Index(pixels[i + 3])];
        pixels[i + 4] = table[toRgb565Index(pixels[i + 4])];
        pixels[i + 5] = table[toRgb565Index(pixels[i + 5])];
        pixels[i + 6] = table[toRgb565Index(pixels[i + 6])];
        pixels[i + 7] = table[toRgb565Index(pixels[i + 7])];
    }
    for (; i < end; ++i) {
        pixels[i] = table[toRgb565Index(pixels[i])];
    }
}

static void processInterpolatedRange(uint32_t* pixels, int start, int end) {
    if (start >= end) {
        return;
    }

    uint32_t* table = grayTable;
    const int sampleStride = 4;
    int sample = start;

    for (; sample + 12 < end; sample += 16) {
        uint32_t g0 = table[toRgb565Index(pixels[sample])];
        uint32_t g1 = table[toRgb565Index(pixels[sample + 4])];
        uint32_t g2 = table[toRgb565Index(pixels[sample + 8])];
        uint32_t g3 = table[toRgb565Index(pixels[sample + 12])];

        write4Same(&pixels[sample], g0);
        write4Same(&pixels[sample + 4], g1);
        write4Same(&pixels[sample + 8], g2);
        write4Same(&pixels[sample + 12], g3);
    }

    for (; sample < end; sample += sampleStride) {
        uint32_t gray = table[toRgb565Index(pixels[sample])];
        int remaining = end - sample;
        if (remaining >= sampleStride) {
            write4Same(&pixels[sample], gray);
            continue;
        }
        for (int k = 0; k < remaining; ++k) {
            pixels[sample + k] = gray;
        }
    }
}

static void processRange(GrayThreadWorkItem* work) {
    if (work->interpolated) {
        processInterpolatedRange(work->pixels, work->start, work->end);
        return;
    }
    processTableRange(work->pixels, work->start, work->end);
}

static void processSingleThread(uint32_t* pixels, int totalPixels, int interpolated) {
    GrayThreadWorkItem work = {
        .pixels = pixels,
        .start = 0,
        .end = totalPixels,
        .interpolated = interpolated
    };
    processRange(&work);
}

static void processWorkerTask(int workerIndex, int workerCount, void* context) {
    GrayProcessContext* processContext = (GrayProcessContext*)context;
    int totalPixels = processContext->totalPixels;
    int baseChunk = totalPixels / workerCount;
    int remainder = totalPixels % workerCount;

    int start = workerIndex * baseChunk + (workerIndex < remainder ? workerIndex : remainder);
    int end = start + baseChunk + (workerIndex < remainder ? 1 : 0);

    GrayThreadWorkItem work = {
        .pixels = processContext->pixels,
        .start = start,
        .end = end,
        .interpolated = processContext->interpolated
    };
    processRange(&work);
}

static void processWithWorkerPool(
    uint32_t* pixels,
    int totalPixels,
    int numThreads,
    int interpolated
) {
    GrayProcessContext context = {
        .pixels = pixels,
        .totalPixels = totalPixels,
        .interpolated = interpolated
    };

    if (!native_thread_pool_run(numThreads, processWorkerTask, &context)) {
        processSingleThread(pixels, totalPixels, interpolated);
    }
}

static void processMultithreaded(
    uint32_t* pixels,
    int totalPixels,
    int requestedThreads,
    int interpolated
) {
    initGrayTable();
    if (grayTable == NULL || totalPixels <= 0) {
        return;
    }

    int numThreads = requestedThreads;
    if (numThreads < 1) {
        numThreads = 1;
    }
    int maxPoolSize = native_thread_pool_max_size();
    if (numThreads > maxPoolSize) {
        numThreads = maxPoolSize;
    }
    if (numThreads > totalPixels) {
        numThreads = totalPixels;
    }

    if (numThreads == 1) {
        processSingleThread(pixels, totalPixels, interpolated);
        return;
    }

    processWithWorkerPool(pixels, totalPixels, numThreads, interpolated);
}

jintArray native_process_grayscale(
    JNIEnv* env,
    jintArray imageData,
    jint width,
    jint height,
    int interpolated
) {
    int totalPixels = width * height;

    jint* pixels = (*env)->GetPrimitiveArrayCritical(env, imageData, NULL);
    if (pixels != NULL) {
        processMultithreaded((uint32_t*)pixels, totalPixels, OPTIMAL_THREAD_COUNT, interpolated);
        (*env)->ReleasePrimitiveArrayCritical(env, imageData, pixels, 0);
        return imageData;
    }

    pixels = (*env)->GetIntArrayElements(env, imageData, NULL);
    if (pixels == NULL) {
        return NULL;
    }
    processMultithreaded((uint32_t*)pixels, totalPixels, OPTIMAL_THREAD_COUNT, interpolated);
    (*env)->ReleaseIntArrayElements(env, imageData, pixels, 0);
    return imageData;
}
