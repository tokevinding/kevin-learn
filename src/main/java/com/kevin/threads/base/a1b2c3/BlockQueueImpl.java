package com.kevin.threads.base.a1b2c3;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author kevin
 * @date 2020-10-19 11:33:40
 * @desc
 */
public class BlockQueueImpl {
    public static void main(String[] args) throws Exception {
        char[] num = "1234567".toCharArray();
        char[] cs = "ABCDEFG".toCharArray();
        Object o = new Object();
        ArrayBlockingQueue queue1 = new ArrayBlockingQueue(1);
        ArrayBlockingQueue queue2 = new ArrayBlockingQueue(1);

        new Thread(() -> {
            queue1.add(cs[0]);
            for (int i = 0; i < num.length; i++) {
                try {
                    System.out.println(queue1.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                queue2.add(num[i]);
            }
        }, "t1").start();

        new Thread(() -> {
            for (int i = 0; i < cs.length; i++) {
                try {
                    System.out.println(queue2.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (i + 1 < (cs.length)) {
                    queue1.add(cs[i+1]);
                }
            }
        }, "t2").start();

    }
}
