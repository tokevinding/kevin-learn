package com.kevin.threads.juc.tools;

import java.util.Random;
import java.util.concurrent.CyclicBarrier;

/**
 * @author Kevin
 * @date 2020-09-17 19:07:31
 * @desc
 */
public class ChairRunnable implements Runnable {
    private final CyclicBarrier cyclicBarrier;
    private final String event;

    public ChairRunnable(CyclicBarrier cyclicBarrier, String event) {
        this.cyclicBarrier = cyclicBarrier;
        this.event = event;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            System.out.println("开始做【" + event + "】。");

            // 等待其他线程完成
            cyclicBarrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("【" + event + "】做好了， 我们来一起组装吧！");
    }
}
