package com.kevin.threads.base.a1b2c3;

import com.kevin.threads.juc.aqs.sub.RwReentrantLock;

import java.util.concurrent.locks.Condition;

/**
 * @author kevin
 * @date 2020-10-19 11:33:40
 * @desc
 */
public class ReentrantLockConditionImpl {
    public static void main(String[] args) throws Exception {
        char[] num = "1234567".toCharArray();
        char[] cs = "ABCDEFG".toCharArray();
        RwReentrantLock lock = new RwReentrantLock();
        Condition condition1 = lock.newCondition();
        Condition condition2 = lock.newCondition();
        new Thread(() -> {
            lock.lock();
            for (char c : cs) {
                System.out.println(c);
                condition2.signal();
                try {
                    condition1.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lock.unlock();
        }, "t1").start();

        new Thread(() -> {
            lock.lock();
            for (char n : num) {
                System.out.println(n);
                condition1.signal();
                try {
                    condition2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lock.unlock();
        }, "t2").start();;
    }
}
