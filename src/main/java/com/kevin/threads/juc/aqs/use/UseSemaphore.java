package com.kevin.threads.juc.aqs.use;

import com.kevin.threads.juc.aqs.sub.RwSemaphore;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author dinghaifeng
 * @date 2021-04-25 18:28:31
 * @desc
 */
public class UseSemaphore {

    public static void main(String[] args) {
        final RwSemaphore semaphore = new RwSemaphore(3);
        int a = 0;
        for (int i = 0; i < 3; i++) {
            final AtomicInteger current = new AtomicInteger(i + 1);
            new Thread(() -> {
                AtomicInteger current1 = current;
                try {
                    semaphore.acquire(2);
                    System.out.println(current1.get() + " - 获取成功, Thread Name: " + Thread.currentThread().getName());
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release(2);
                    System.out.println(current1.get() + " - 释放成功, Thread Name: " + Thread.currentThread().getName());
                }
            }).start();
        }
    }

}
