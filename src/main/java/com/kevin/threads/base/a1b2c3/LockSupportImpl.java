package com.kevin.threads.base.a1b2c3;

import java.util.concurrent.locks.LockSupport;

/**
 * @author dinghaifeng
 * @date 2020-10-19 10:05:07
 * @desc
 */
public class LockSupportImpl {
    static Thread t1 = null;
    static Thread t2 = null;
    public static void main(String[] args) {
        char[] num = "1234567".toCharArray();
        char[] cs = "ABCDEFG".toCharArray();

        t1 = new Thread(() -> {
            for (char c : cs) {

                System.out.println(c);

                LockSupport.unpark(t2);
                LockSupport.park();
            }
        }, "t1");

        t2 = new Thread(() -> {
            for (char n : num) {
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LockSupport.park();
                System.out.println(n);
                LockSupport.unpark(t1);
            }
        }, "t2");
        t2.start();
        t1.start();

    }
}
