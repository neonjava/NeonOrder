package me.neonjava.neonorder.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DispatchUtil {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void async(Runnable toRun) {
        executor.submit(toRun);
    }

    public static void shutdown() {
        executor.shutdown();
    }
}
