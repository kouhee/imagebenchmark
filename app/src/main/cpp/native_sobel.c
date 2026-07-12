#include "native_sobel.h"

#include <android/log.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define LOG_TAG "NativeSobel"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static inline int gray(uint32_t pixel) {
    int r = (int)((pixel >> 16) & 0xFFU);
    int g = (int)((pixel >> 8) & 0xFFU);
    int b = (int)(pixel & 0xFFU);
    return (r + g + b) / 3;
}

static void processSobelBasic(uint32_t* pixels, int width, int height) {
    int totalPixels = width * height;
    if (totalPixels <= 0) {
        return;
    }

    uint32_t* source = (uint32_t*)malloc((size_t)totalPixels * sizeof(uint32_t));
    if (source == NULL) {
        LOGE("Failed to allocate Sobel source buffer");
        return;
    }
    memcpy(source, pixels, (size_t)totalPixels * sizeof(uint32_t));

    for (int y = 0; y < height; ++y) {
        int row = y * width;
        if (y == 0 || y == height - 1) {
            for (int x = 0; x < width; ++x) {
                pixels[row + x] = 0xFF000000U;
            }
            continue;
        }

        pixels[row] = 0xFF000000U;
        pixels[row + width - 1] = 0xFF000000U;

        for (int x = 1; x < width - 1; ++x) {
            int p00 = gray(source[(y - 1) * width + (x - 1)]);
            int p01 = gray(source[(y - 1) * width + x]);
            int p02 = gray(source[(y - 1) * width + (x + 1)]);
            int p10 = gray(source[y * width + (x - 1)]);
            int p12 = gray(source[y * width + (x + 1)]);
            int p20 = gray(source[(y + 1) * width + (x - 1)]);
            int p21 = gray(source[(y + 1) * width + x]);
            int p22 = gray(source[(y + 1) * width + (x + 1)]);

            int gx = (-p00 + p02) + (-2 * p10 + 2 * p12) + (-p20 + p22);
            int gy = (p00 + 2 * p01 + p02) - (p20 + 2 * p21 + p22);

            int magnitude = (int)sqrt((double)(gx * gx + gy * gy));
            if (magnitude > 255) {
                magnitude = 255;
            }

            pixels[row + x] =
                0xFF000000U | ((uint32_t)magnitude << 16) | ((uint32_t)magnitude << 8) | (uint32_t)magnitude;
        }
    }

    free(source);
}

jintArray native_process_sobel(
    JNIEnv* env,
    jintArray imageData,
    jint width,
    jint height
) {
    jint* pixels = (*env)->GetPrimitiveArrayCritical(env, imageData, NULL);
    if (pixels != NULL) {
        processSobelBasic((uint32_t*)pixels, width, height);
        (*env)->ReleasePrimitiveArrayCritical(env, imageData, pixels, 0);
        return imageData;
    }

    pixels = (*env)->GetIntArrayElements(env, imageData, NULL);
    if (pixels == NULL) {
        return NULL;
    }
    processSobelBasic((uint32_t*)pixels, width, height);
    (*env)->ReleaseIntArrayElements(env, imageData, pixels, 0);
    return imageData;
}
