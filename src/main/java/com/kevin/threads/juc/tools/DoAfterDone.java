package com.kevin.threads.juc.tools;

import com.kevin.threads.juc.pools.FaceThreadPool;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kevin
 * @date 2020-09-17 19:06:46
 * @desc
 */
public class DoAfterDone {

    public static void main(String[] args) {
        ThreadPoolExecutor executorService = FaceThreadPool.getExecutorService();
        AtomicInteger totalDone = new AtomicInteger(0);
        for (int i = 0; i < 30; i++) {
            // 申明，等待线程数量 3次
            AtomicInteger doneCount = new AtomicInteger(0);
            int totalCount = 3;
            executorService.execute(() -> run(doneCount, "椅子腿", totalCount, totalDone));
            executorService.execute(() -> run(doneCount, "椅子面", totalCount, totalDone));
            executorService.execute(() -> run(doneCount, "椅子背", totalCount, totalDone));
        }
        executorService.shutdown();
        System.out.println("主线程结束");
    }

    public static void run(AtomicInteger doneCount, String event, int totalCount, AtomicInteger totalDone) {
        try {
//            Thread.sleep(100);
//            System.out.println("开始做【" + event + "】。");
            Thread.sleep(20);
//            System.out.println("【" + event + "】做好了， 我们来一起组装吧！");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            doneCount.getAndIncrement();
            if (doneCount.compareAndSet(totalCount, 1 + totalCount)) {
                //释放锁逻辑
                System.out.println("释放锁逻辑"+ totalDone.incrementAndGet());
            }
        }
    }
}
