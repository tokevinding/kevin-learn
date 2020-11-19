package com.kevin.java.queue.delay;

import com.kevin.threads.juc.pools.FaceThreadPool;
import com.kevin.tools.utils.ConsoleOutputUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.Optional;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author kevin
 * @date 2020-10-12 11:41:31
 * @desc 延时队列
 */
public class FaceDelayQueue {

    public static void main(String[] args) throws InterruptedException {
        jdkDelay();
    }

    /**
     * JDK延时队列
     * 1.队列节点需要实现 Delayed
     * 2.getDelay()获取剩余时间
     */
    public static void jdkDelay() {
        DelayQueue<Delayed> queue = new DelayQueue<>();
        ThreadPoolExecutor executorService = FaceThreadPool.getExecutorService();
        executorService.execute(() -> {
            for (int i = 1; i <= 10; i++) {
                queue.put(new DelayNode(String.valueOf(i * 10), TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS) + System.nanoTime()));
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Add done");
        });

        executorService.execute(() -> {
            long start = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                try {
                    long end = System.nanoTime();
                    System.out.println(TimeUnit.NANOSECONDS.toMillis(end - start) + "ms | poll | " + Optional.ofNullable(((DelayNode)queue.take())).map(DelayNode::getOrderId).orElse(""));
                    start = end;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Take done");
        });
        executorService.shutdown();
    }

    @Data
    @AllArgsConstructor
    static class DelayNode implements Delayed {

        @Getter
        private String orderId;

        private long timeout;

        @Override
        public long getDelay(TimeUnit unit) {
            long convert = unit.convert(timeout - System.nanoTime(), TimeUnit.NANOSECONDS);
//            System.out.println(convert);
            return convert;
        }

        @Override
        public int compareTo(Delayed o) {
            if (o == this) {
                return 0;
            }
            if (!(o instanceof DelayNode)) {
                throw new RuntimeException("类型不匹配");
            }
            DelayNode oN = (DelayNode) o;
            long r = getDelay(TimeUnit.NANOSECONDS) - oN.getDelay(TimeUnit.NANOSECONDS);
            return r == 0 ? 0 : (r < 0) ? -1 : 1;
        }

        public void processOrder() {
            ConsoleOutputUtils.hr("开始处理订单: " + orderId);
        }
    }
}
