package com.kevin.threads.juc.pools;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Kevin
 * @date 2020-09-11 18:48:14
 * @desc
 */
public class ThreadPoolHome {
    public static void main(String[] args) throws Exception {

//        runCore(getDefaultPool());
        runBeyondCoreAndInQueue(getDefaultPool());
//        runBeyondQueueAndInMax(getDefaultPool());
//        runOutMax(getDefaultPool());
    }

    public static ThreadPoolExecutor getDefaultPool() {
        ThreadPoolExecutor.AbortPolicy handler = new ThreadPoolExecutor.AbortPolicy();
        return new ThreadPoolExecutor(1, 2
                , 6, TimeUnit.SECONDS
                , new ArrayBlockingQueue<Runnable>(1)
                , Executors.defaultThreadFactory(), handler);
    }

    /**
     * 任务数 <= 核心线程数
     */
    public static void runCore(ThreadPoolExecutor executorService) {
        runTask(executorService, 1);
    }

    /**
     * 任务数 > 核心线程数 & 任务数 <= (核心线程数 + 队列数)
     */
    public static void runBeyondCoreAndInQueue(ThreadPoolExecutor executorService) {
        runTask(executorService, 2);
    }

    /**
     * 任务数 > (核心线程数 + 队列数) & 任务数 <= (最大线程数 + 队列数)
     */
    public static void runBeyondQueueAndInMax(ThreadPoolExecutor executorService) {
        runTask(executorService, 3);
    }

    /**
     * 任务数 > (最大线程数 + 队列数)
     */
    public static void runOutMax(ThreadPoolExecutor executorService) {
        runTask(executorService, 4);
    }

    public static void runTask(ThreadPoolExecutor executorService, int taskCount) {
        getParameterIndex(executorService);
        for (int i = 0; i < taskCount; i++) {
            executorService.execute(new TaskRunnable(i, executorService));
        }
        try {
            //睡50ms，确保启动完成
            Thread.sleep(50L);
            //验证 keepAliveTime: 6s
            System.out.println("++ 睡6s前活动线程数：" + executorService.getActiveCount() + " | 线程池大小：" + executorService.getPoolSize());
            Thread.sleep(4000 + 6000);
            //验证 6s 后活动线程减少为核心数量
            System.out.println("-- > 睡6s后活动线程数：" + executorService.getActiveCount() + " | 线程池大小：" + executorService.getPoolSize());
        } catch (InterruptedException e) {
            System.out.println("Out sleep!!");
        }
        executorService.shutdown();
    }

    /**
     * 守护线程，获取各个参数指标
     */
    public static void getParameterIndex(ThreadPoolExecutor executorService) {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    //睡100ms，确保启动完成
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    System.out.println("Out sleep!!");
                }
                System.out.println("****" + " | 开始睡眠 | 队列数量："
                        + executorService.getQueue().size() + " | 活动线程数：" + executorService.getActiveCount()
                        + " | 线程池大小：" + executorService.getPoolSize() + "****");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

}
