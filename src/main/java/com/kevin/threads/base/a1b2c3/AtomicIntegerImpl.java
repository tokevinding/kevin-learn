package com.kevin.threads.base.a1b2c3;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kevin
 * @date 2020-10-19 11:33:40
 * @desc
 */
public class AtomicIntegerImpl {
    public static void main(String[] args) {
        char[] num = "1234567".toCharArray();
        char[] cs = "ABCDEFG".toCharArray();

        AtomicInteger flag = new AtomicInteger();

        new Thread(() -> {
            for (char c : cs) {
                while (flag.get() != 0) {
                }
                System.out.println(c);
                flag.incrementAndGet();
            }
        }, "t1").start();

        new Thread(() -> {
            for (char n : num) {
                while (flag.get() != 1) {
                }
                System.out.println(n);
                flag.decrementAndGet();
            }
        }, "t2").start();

    }
}
