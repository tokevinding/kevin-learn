package com.kevin.threads.juc.pools;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Kevin
 * @date 2020-09-13 21:27:51
 * @desc
 */
public class TaskRunnable implements Runnable {
    private int currentSerial;
    private ThreadPoolExecutor executorService;

    public TaskRunnable(int currentSerial, ThreadPoolExecutor executorService) {
        this.currentSerial = currentSerial;
        this.executorService = executorService;
    }

    @Override
    public void run() {
        try {
            System.out.println("< " + currentSerial + " | 开始睡眠 | 队列数量："
                    + executorService.getQueue().size() + " | 活动线程数：" + executorService.getActiveCount()
                    + " | 线程池大小：" + executorService.getPoolSize());
            //每个线程睡2s
            Thread.sleep(2000L);
            System.out.println("> " + currentSerial + " | 睡眠完成 | 队列数量："
                    + executorService.getQueue().size() + " | 活动线程数：" + executorService.getActiveCount()
                    + " | 线程池大小：" + executorService.getPoolSize());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
