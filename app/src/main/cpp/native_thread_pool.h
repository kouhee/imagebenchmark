#ifndef NATIVE_THREAD_POOL_H
#define NATIVE_THREAD_POOL_H

typedef void (*native_thread_pool_task_fn)(int worker_index, int worker_count, void* context);

int native_thread_pool_run(int worker_count, native_thread_pool_task_fn task_fn, void* context);
int native_thread_pool_max_size(void);
void native_thread_pool_shutdown(void);

#endif
