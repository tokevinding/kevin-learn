package com.kevin.threads.juc.pools;

import java.util.concurrent.*;

/**
 * @author Kevin
 * @date 2020-09-11 18:48:14
 * @desc
 */
public class FaceThreadPool {
    /**
     * 核心线程数
     */
    private static int corePoolSize = 3;
    /**
     * 最大线程数
     */
    private static int maximumPoolSize = 3;
    /**
     * 超出核心线程数的空闲线程，存活时间规格
     */
    private static long keepAliveTime = 6;
    /**
     * 超出核心线程数的空闲线程，存活时间单位
     */
    private static TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 需要执行的 超出 核心线程数 的任务 需要加入的阻塞队列
     */
    private static BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(1000);
    /**
     * 线程工厂
     */
    private static ThreadFactory threadFactory = Executors.defaultThreadFactory();
    /**
     * 超出（核心线程数 + 队列大小 + （最大线程数 — 核心线程数））的任务 需要执行的处理方案
     */
    private static RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();

    /**
     * 创建一个线程池执行器
     * @return
     */
    public static ThreadPoolExecutor getExecutorService() {
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(corePoolSize
                , maximumPoolSize, keepAliveTime, timeUnit, workQueue, threadFactory, handler);
        return executorService;
    }
}
