package com.kevin.threads.juc.pools.analyse;

/**
 * @author dinghaifeng
 * @date 2020-10-23 12:55:29
 * @desc
 */
public class AnalyseThreadSafe extends BaseAnalyse {
    public static void main(String[] args) {
        AnalyseThreadSafe threadSafe = new AnalyseThreadSafe();
        threadSafe.ctl.compareAndSet(ctlOf(RUNNING, 0), ctlOf(STOP, 0));
        new Thread(() -> {
            int c = threadSafe.ctl.get();
            int c1 = threadSafe.flag;
            sleep(3000L);
            System.out.println(threadSafe.flag + " | "+ c1);
            System.out.println("son  thread: " + Integer.toBinaryString(c));
        }).start();
        sleep(1000L);
        threadSafe.flag = 10;
        threadSafe.ctl.incrementAndGet();
        System.out.println("main thread: " + Integer.toBinaryString(threadSafe.ctl.get()));
    }

    volatile int flag = 1;

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
