package com.example.musicplayer.utility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class ThreadPoolUtil {
    private static final int CORE_THREAD_COUNT = 3;

    private static ExecutorService executor = Executors.newFixedThreadPool(CORE_THREAD_COUNT);
    private static ScheduledExecutorService scheduledExecutor;

    private ThreadPoolUtil() {
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        if (null == scheduledExecutor) {
            synchronized (ScheduledExecutorService.class){
                if(null == scheduledExecutor)
                    scheduledExecutor = Executors.newScheduledThreadPool(2);
            }
        }
        return scheduledExecutor;
    }

    public static ExecutorService getInstance() {
        return executor;
    }

    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void shutdownNow() {
        executor.shutdownNow();
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }
}