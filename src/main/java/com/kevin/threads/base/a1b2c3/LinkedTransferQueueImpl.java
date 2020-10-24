package com.kevin.threads.base.a1b2c3;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

/**
 * @author dinghaifeng
 * @date 2020-10-19 17:38:22
 * @desc
 */
public class LinkedTransferQueueImpl {
    public static void main(String[] args) throws Exception {
        char[] num = "1234567".toCharArray();
        char[] cs = "ABCDEFG".toCharArray();
        Object o = new Object();
        LinkedTransferQueue queue = new LinkedTransferQueue();

        new Thread(() -> {
            for (int i = 0; i < cs.length; i++) {
                try {
                    System.out.println(queue.take());
                    queue.transfer(num[i]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "t1").start();

        new Thread(() -> {
            for (int i = 0; i < num.length; i++) {
                try {
                    System.out.println(queue.take());
                    queue.transfer(cs[i]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "t2").start();

    }
}
