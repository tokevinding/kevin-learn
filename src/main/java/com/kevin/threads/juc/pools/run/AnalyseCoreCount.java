package com.kevin.threads.juc.pools.run;

import com.kevin.threads.juc.pools.analyse.BaseAnalyse;
import com.kevin.threads.juc.pools.rw.RwThreadPoolExecutor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dinghaifeng
 * @date 2020-11-18 17:24:31
 * @desc 分析核心线程数
 */
public class AnalyseCoreCount extends BaseAnalyse {
    public static void main(String[] args) {

//        testCommonThreadCount();

//        testPreCoreThreadCount();

        testShutdownAddTask();

    }


    /**
     * 测试 Shutdown 状态下添加线程
     */
    private static void testShutdownAddTask() {
        RwThreadPoolExecutor pool = getPool();
        printCount(1, pool);
        //3个任务 - 单任务10s（开两个线程 - 其中一个任务添加到队列 但shutdown状态也会被执行）
        addTask(pool, 6 * 1000, 1);
        addTask(pool, 10 * 1000, 2);
        addTask(pool, 10 * 1000, 3);
        printCount(1, pool);

        //改变状态
        pool.shutdown();

        //添加4号任务 - 本任务会被拒绝
        addTask(pool, 3 * 1000, 4);
        printCount(1, pool);
    }

    /**
     * 测试预热线程数
     */
    private static void testPreCoreThreadCount() {
        RwThreadPoolExecutor pool = getPool();
        int coreCount = pool.prestartAllCoreThreads();
        printCount(1, pool);
        pool.shutdown();
    }

    /**
     * 测试正常情况下的线程数量
     */
    private static void testCommonThreadCount() {
        RwThreadPoolExecutor pool = getPool();
        printCount(1, pool);

        //睡4s
//        sleep(4 * 1000);
//        printCount(2, pool);

        //添加1s任务 - 加线程
        addTask(pool, 10 * 1000, 1);
        sleepPrintCount(3, pool);

        //添加1s任务 - 加线程
        addTask(pool, 10 * 1000, 2);
        sleepPrintCount(4, pool);

        //添加1s任务 - 加到队列
        addTask(pool, 10 * 1000, 3);
        sleepPrintCount(5, pool);

        //添加1s任务 - 继续加线程
        addTask(pool, 10 * 1000, 4);
        sleepPrintCount(6, pool);
        println("---------------------------------");

        pool.shutdown();
    }


    private static void sleepPrintCount(int idx, RwThreadPoolExecutor pool) {
        sleep(100);
        println("===核心线程数量" + idx + "：CorePoolSize: " + pool.getCorePoolSize() + " | currentCount: " + workerCountOf(pool.getCtl().get()) + "===");
    }

    private static void printCount(int idx, RwThreadPoolExecutor pool) {
        println("===核心线程数量" + idx + "：CorePoolSize: " + pool.getCorePoolSize() + " | currentCount: " + workerCountOf(pool.getCtl().get()) + "===");
    }


    /**
     * 添加多个任务
     */
    private static void addTasks(RwThreadPoolExecutor pool, int executeMs, int count) {
        for (int i = 1; i <= count; i++) {
            addTask(pool, executeMs, i);
        }
    }

    private static void addTask(RwThreadPoolExecutor pool, int executeMs, int num) {
        pool.execute(() -> {
            println("任务" + num + "开始执行");
            sleep(executeMs);
            println("任务" + num +"执行完了");
        });
    }

    private static RwThreadPoolExecutor getPool() {
         return new RwThreadPoolExecutor(2, 3
                , 30L, TimeUnit.SECONDS
                , new ArrayBlockingQueue<>(1)
                , new PersonalThreadFactory()
        );
    }

    private static void sleep(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void println(String content) {
        System.out.println(content);
    }



    /**
     * The default thread factory
     */
    static class PersonalThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        PersonalThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "-kevin-pool-" + poolNumber.getAndIncrement() + "-kevin-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
