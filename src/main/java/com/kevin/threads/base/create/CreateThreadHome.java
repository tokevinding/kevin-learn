package com.kevin.threads.base.create;

import com.kevin.threads.juc.pools.FaceThreadPool;
import com.kevin.tools.utils.ConsoleOutputUtils;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Kevin
 * @date 2020-09-11 18:47:28
 * @desc
 */
public class CreateThreadHome {
    public static void main(String[] args) throws Exception {
//        start1();
//        start2();
//        start3();
        start4();
    }

    public static void start1() {
        new OneExtendThread().start();
    }
    public static void start2() {
        new Thread(new TwoImplRunnable()).start();
    }

    public static void start3() throws Exception {
        FutureTask<String> futureTask = new FutureTask<>(new ThreeImplCallable());
        futureTask.run();
        String s = futureTask.get();
        System.out.println(s);
    }

    public static void start4() throws Exception {
        ThreadPoolExecutor executorService = FaceThreadPool.getExecutorService();
        executorService.submit(new TwoImplRunnable());
        ConsoleOutputUtils.hr();
        Future<String> future = executorService.submit(new ThreeImplCallable());
        ConsoleOutputUtils.hr(future.get());
        executorService.shutdownNow();
    }
}
