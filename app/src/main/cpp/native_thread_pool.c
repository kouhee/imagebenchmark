#include "native_thread_pool.h"
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <sys/prctl.h>
#include <android/trace.h>

#define POOL_SIZE 4

typedef struct {
    pthread_t threads[POOL_SIZE];
    pthread_mutex_t mutex;
    pthread_cond_t cond_work;
    pthread_cond_t cond_done;
    native_thread_pool_task_fn task_fn;
    void* context;
    int initialized;
    int shutdown;
    int generation;
    int active_workers;
    int completed_workers;
} NativeThreadPool;

static NativeThreadPool g_pool = {
    .mutex = PTHREAD_MUTEX_INITIALIZER,
    .cond_work = PTHREAD_COND_INITIALIZER,
    .cond_done = PTHREAD_COND_INITIALIZER,
    .task_fn = NULL,
    .context = NULL,
    .initialized = 0,
    .shutdown = 0,
    .generation = 0,
    .active_workers = 0,
    .completed_workers = 0
};

static void* worker_loop(void* arg) {
    int worker_index = (int)(intptr_t)arg;
    int observed_generation = 0;

    // Set thread name for easier identification in Perfetto/systrace
    char thread_name[16];
    snprintf(thread_name, sizeof(thread_name), "NativeWorker-%d", worker_index);
    prctl(PR_SET_NAME, thread_name);

    pthread_mutex_lock(&g_pool.mutex);
    for (;;) {
        while (!g_pool.shutdown && g_pool.generation == observed_generation) {
            pthread_cond_wait(&g_pool.cond_work, &g_pool.mutex);
        }

        if (g_pool.shutdown) {
            pthread_mutex_unlock(&g_pool.mutex);
            return NULL;
        }

        observed_generation = g_pool.generation;
        if (worker_index >= g_pool.active_workers || g_pool.task_fn == NULL) {
            continue;
        }

        native_thread_pool_task_fn task_fn = g_pool.task_fn;
        void* context = g_pool.context;
        int active_workers = g_pool.active_workers;
        pthread_mutex_unlock(&g_pool.mutex);

        char worker_trace_name[32];
        snprintf(worker_trace_name, sizeof(worker_trace_name), "Worker-%d", worker_index);

        ATrace_beginSection(worker_trace_name);
        task_fn(worker_index, active_workers, context);
        ATrace_endSection();

        pthread_mutex_lock(&g_pool.mutex);
        g_pool.completed_workers++;
        if (g_pool.completed_workers >= g_pool.active_workers) {
            pthread_cond_signal(&g_pool.cond_done);
        }
    }
}

static int ensure_initialized(void) {
    pthread_mutex_lock(&g_pool.mutex);
    if (g_pool.initialized) {
        pthread_mutex_unlock(&g_pool.mutex);
        return 1;
    }

    int created = 0;
    for (int i = 0; i < POOL_SIZE; i++) {
        if (pthread_create(&g_pool.threads[i], NULL, worker_loop, (void*)(intptr_t)i) != 0) {
            g_pool.shutdown = 1;
            pthread_cond_broadcast(&g_pool.cond_work);
            pthread_mutex_unlock(&g_pool.mutex);
            for (int j = 0; j < created; j++) {
                pthread_join(g_pool.threads[j], NULL);
            }
            pthread_mutex_lock(&g_pool.mutex);
            g_pool.shutdown = 0;
            pthread_mutex_unlock(&g_pool.mutex);
            return 0;
        }
        created++;
    }

    g_pool.initialized = 1;
    pthread_mutex_unlock(&g_pool.mutex);
    return 1;
}

int native_thread_pool_run(int worker_count, native_thread_pool_task_fn task_fn, void* context) {
    if (worker_count < 1 || task_fn == NULL) {
        return 0;
    }

    if (!ensure_initialized()) {
        return 0;
    }

    if (worker_count > POOL_SIZE) {
        worker_count = POOL_SIZE;
    }

    pthread_mutex_lock(&g_pool.mutex);
    g_pool.task_fn = task_fn;
    g_pool.context = context;
    g_pool.active_workers = worker_count;
    g_pool.completed_workers = 0;
    g_pool.generation++;
    pthread_cond_broadcast(&g_pool.cond_work);

    while (g_pool.completed_workers < g_pool.active_workers) {
        pthread_cond_wait(&g_pool.cond_done, &g_pool.mutex);
    }

    pthread_mutex_unlock(&g_pool.mutex);
    return 1;
}

int native_thread_pool_max_size(void) {
    return POOL_SIZE;
}

void native_thread_pool_shutdown(void) {
    pthread_mutex_lock(&g_pool.mutex);
    if (!g_pool.initialized) {
        pthread_mutex_unlock(&g_pool.mutex);
        return;
    }
    g_pool.shutdown = 1;
    pthread_cond_broadcast(&g_pool.cond_work);
    pthread_mutex_unlock(&g_pool.mutex);

    for (int i = 0; i < POOL_SIZE; i++) {
        pthread_join(g_pool.threads[i], NULL);
    }

    pthread_mutex_lock(&g_pool.mutex);
    g_pool.initialized = 0;
    g_pool.shutdown = 0;
    g_pool.task_fn = NULL;
    g_pool.context = NULL;
    g_pool.active_workers = 0;
    g_pool.completed_workers = 0;
    pthread_mutex_unlock(&g_pool.mutex);
}
