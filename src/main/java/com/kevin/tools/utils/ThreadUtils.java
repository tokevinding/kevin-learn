package com.kevin.tools.utils;

import com.kevin.threads.juc.cas.DoTask;
import com.kevin.threads.juc.pools.FaceThreadPool;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Kevin
 * @date 2020-09-13 23:41:46
 * @desc
 */
public class ThreadUtils {

    /**
     * 多线程处理任务
     */
    public static void multiThreadProcess(DoTask doTask) {
        //下面的线程池会有问题
        ThreadPoolExecutor executorService = FaceThreadPool.getExecutorService();
        executorService.setCorePoolSize(100);
        executorService.setMaximumPoolSize(100);
        for (int i = 0; i < 100; i++) {
            executorService.submit(doTask::doTask);
        }
        executorService.shutdown();
        while (executorService.getActiveCount() > 0) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
