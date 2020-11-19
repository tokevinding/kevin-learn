package com.kevin.threads.juc.pools.analyse;

import com.kevin.tools.utils.ConsoleOutputUtils;

/**
 * @author kevin
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
            int c2 = threadSafe.value;
            sleep(3000L);
            System.out.println(threadSafe.flag + " | "+ c1);
            System.out.println(threadSafe.value + " | "+ c2);
            System.out.println("son  thread: " + Integer.toBinaryString(c));
        }).start();
        sleep(1000L);
        threadSafe.flag = 10;
        threadSafe.ctl.incrementAndGet();
        threadSafe.value = 10;
        System.out.println("main thread-flag: " + threadSafe.flag);
        System.out.println("main thread-value: " + threadSafe.value);
        System.out.println("main thread: " + Integer.toBinaryString(threadSafe.ctl.get()));
        ConsoleOutputUtils.hr();
    }

    volatile int flag = 999999999;
    volatile Integer value = new Integer(999999999);
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
