package com.kevin.threads.base.common;

import java.util.concurrent.TimeUnit;

/**
 * @author dinghaifeng
 * @date 2021-08-31 17:36:20
 * @desc
 */
public class FaceJoin {
    public static void main(String[] args) {
        FaceJoin lock = new FaceJoin();
        Thread t1 = new Thread(() -> {
            try {
                System.out.println("t1开始执行");
                TimeUnit.SECONDS.sleep(5);
                System.out.println("t1执行完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                System.out.println("t2执行 -- t1.join()");
                t1.join();
                System.out.println("t2开始执行");
                TimeUnit.SECONDS.sleep(2);
                System.out.println("t2执行完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.start();
        t2.start();
        System.out.println("主流程完成");
    }
}
