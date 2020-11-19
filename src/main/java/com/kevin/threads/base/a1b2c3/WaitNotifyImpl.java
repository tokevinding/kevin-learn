package com.kevin.threads.base.a1b2c3;

/**
 * @author kevin
 * @date 2020-10-19 11:33:40
 * @desc
 */
public class WaitNotifyImpl {
    public static void main(String[] args) throws Exception {
        char[] num = "1234567".toCharArray();
        char[] cs = "ABCDEFG".toCharArray();
        Object o = new Object();

        new Thread(() -> {
            synchronized (o) {
                for (char c : cs) {
                    System.out.println(c);
                    o.notify();
                    try {
                        o.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "t1").start();

        new Thread(() -> {
            synchronized (o) {
                for (char n : num) {
                    System.out.println(n);
                    o.notify();
                    try {
                        o.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "t2").start();

    }
}
