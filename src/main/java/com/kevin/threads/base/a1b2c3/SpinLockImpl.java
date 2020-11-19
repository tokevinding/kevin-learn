package com.kevin.threads.base.a1b2c3;

/**
 * @author kevin
 * @date 2020-10-19 11:33:40
 * @desc 自旋锁实现
 */
public class SpinLockImpl {
    private static volatile boolean switchFlag = true;
    public static void main(String[] args) {
        char[] num = "1234567".toCharArray();
        char[] cs = "ABCDEFG".toCharArray();

        new Thread(() -> {
            for (char c : cs) {
                while (!switchFlag) {
                }
                System.out.println(c);
                switchFlag = false;
            }
        }, "t1").start();

        new Thread(() -> {
            for (char n : num) {
                while (switchFlag) {
                }
                System.out.println(n);
                switchFlag = true;
            }
        }, "t2").start();

    }
}
