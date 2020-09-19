package com.kevin.threads.juc.tools;

import com.kevin.threads.juc.pools.FaceThreadPool;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author dinghaifeng
 * @date 2020-09-17 19:06:46
 * @desc
 */
public class FaceCyclicBarrier {

    public static void main(String[] args) {
        // 申明，等待线程数量 3次
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3);
        ThreadPoolExecutor executorService = FaceThreadPool.getExecutorService();
        executorService.execute(() -> run(cyclicBarrier, "椅子腿"));
        executorService.execute(() -> run(cyclicBarrier, "椅子面"));
        executorService.execute(() -> run(cyclicBarrier, "椅子背"));
        System.out.println("主线程结束");
        executorService.shutdown();
    }

    public static void run(CyclicBarrier cyclicBarrier, String event) {
        try {
            // 等待其他线程开始
            cyclicBarrier.await();

            System.out.println("开始做【" + event + "】。");
            Thread.sleep(1000);

            // 等待其他线程完成
            cyclicBarrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("【" + event + "】做好了， 我们来一起组装吧！");
    }
}
