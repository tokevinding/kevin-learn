package com.kevin.threads.juc.aqs.analyse;

import com.kevin.threads.juc.aqs.rw.RwReentrantLock;
import com.kevin.threads.juc.pools.rw.RwThreadPoolExecutor;

import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author dinghaifeng
 * @date 2020-10-26 10:29:22
 * @desc
 */
public class AnalyseMultiRequire {
    public static void main(String[] args) {
        RwReentrantLock lock = new RwReentrantLock();
        //两个核心线程
        RwThreadPoolExecutor executor = new RwThreadPoolExecutor(2
                , 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20));

        //1.加锁两次-解锁一次
        executor.execute(() -> {
            lock.lock();
            lock.lock();
            System.out.println("thread 1 print");
//            lock.unlock();
            lock.unlock();
        });
        //2.加锁一次-解锁一次
        Thread t2 = new Thread(() -> {
            lock.lock();
            System.out.println("thread 2 print");
            lock.unlock();
        });
        t2.start();
//        executor.execute(() -> {
//            lock.lock();
//            System.out.println("thread 2 print");
//            lock.unlock();
//        });
//        executor.shutdown();
        //并未终止（只是标记终止）
        executor.shutdownNow();
        System.out.println("池中总worker数："+executor.getWorkers().size());
        System.out.println("池中队列总数："+executor.getQueue().size());
        System.out.println("AQS中队列总数："+lock.getQueueLength());

        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //执行unpark t2 - 程序会继续执行（请求获取锁）
        //此处 获取锁会失败（因为线程1，当前的state = 1），此处会再次park
        LockSupport.unpark(t2);
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //设置state = 1
        lock.setState(0);
        //再次执行unpark t2（t2 获取锁成功 - 顺利执行）
        LockSupport.unpark(t2);
    }
}
