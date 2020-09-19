package com.kevin.threads.juc.tools;

import com.kevin.threads.juc.pools.FaceThreadPool;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dinghaifeng
 * @date 2020-09-17 19:06:46
 * @desc
 */
public class DoAfterDone {

    public static void main(String[] args) {
        // 申明，等待线程数量 3次
        AtomicInteger doneCount = new AtomicInteger(0);
        int totalCount = 3;
        ThreadPoolExecutor executorService = FaceThreadPool.getExecutorService();
        executorService.execute(() -> run(doneCount, "椅子腿", totalCount));
        executorService.execute(() -> run(doneCount, "椅子面", totalCount));
        executorService.execute(() -> run(doneCount, "椅子背", totalCount));
        System.out.println("主线程结束");
        executorService.shutdown();
    }

    public static void run(AtomicInteger doneCount, String event, int totalCount) {
        try {
            Thread.sleep(100);
            System.out.println("开始做【" + event + "】。");
            Thread.sleep(2000);
            System.out.println("【" + event + "】做好了， 我们来一起组装吧！");
            doneCount.getAndIncrement();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (doneCount.compareAndSet(totalCount, 1 + totalCount)) {
                //释放锁逻辑
                System.out.println("释放锁逻辑");
            }
        }
    }
}
