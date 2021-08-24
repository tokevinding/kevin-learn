package com.kevin.threads.juc.aqs.use;

import com.kevin.threads.juc.aqs.sub.blockQueue.RwArrayBlockingQueue;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author dinghaifeng
 * @date 2021-04-25 18:28:31
 * @desc 阻塞队列测试使用
 */
public class UseBlockQueue {

    public static void main(String[] args) {
//        block();
//        noBlock();
//        exception();
        timeOut();
    }


    public static void timeOut() {
        RwArrayBlockingQueue<Integer> blockingQueue = new RwArrayBlockingQueue<>(2);
        UseUtils.set(e -> {
            try {
                boolean offer = blockingQueue.offer(e, 1, TimeUnit.SECONDS);
                if (!offer) {
                    UseUtils.printThread("offer", false, e);
                } else {
                    UseUtils.printThread("offer", true, e);
                }
            } catch (Exception ex) {
                UseUtils.printThread("offer exception", false, e);
            }
        });
        UseUtils.get(() -> {
            try {
                Integer take = blockingQueue.poll(1, TimeUnit.SECONDS);
                if (Objects.isNull(take)) {
                    UseUtils.printThread("poll", false, take);
                } else {
                    UseUtils.printThread("poll", true, take);
                }
                return take;
            } catch (Exception ex) {
                UseUtils.printThread("poll exception", false, "");
                return null;
            }
        });
    }

    public static void exception() {
        RwArrayBlockingQueue<Integer> blockingQueue = new RwArrayBlockingQueue<>(2);
        UseUtils.set(e -> {
            try {
                boolean r = blockingQueue.add(e);
                if (!r) {
                    UseUtils.printThread("add", false, e);
                } else {
                    UseUtils.printThread("add", true, e);
                }
            } catch (Exception ex) {
                UseUtils.printThread("add exception", false, e);
            }
        });
        UseUtils.get(() -> {
            try {
                Integer take = blockingQueue.remove();
                if (Objects.isNull(take)) {
                    UseUtils.printThread("remove", false, take);
                } else {
                    UseUtils.printThread("remove", true, take);
                }
                return take;
            } catch (Exception ex) {
                UseUtils.printThread("remove exception", true, "");
                return null;
            }
        });
    }

    public static void noBlock() {
        RwArrayBlockingQueue<Integer> blockingQueue = new RwArrayBlockingQueue<>(2);
        UseUtils.set(e -> {
            boolean offer = blockingQueue.offer(e);
            if (!offer) {
                UseUtils.printThread("offer", false, e);
            } else {
                UseUtils.printThread("offer", true, e);
            }
        });
        UseUtils.get(() -> {
            Integer take = blockingQueue.poll();
            if (Objects.isNull(take)) {
                UseUtils.printThread("poll", false, take);
            } else {
                UseUtils.printThread("poll", true, take);
            }
            return take;}
        );
    }

    public static void block() {
        RwArrayBlockingQueue<Integer> blockingQueue = new RwArrayBlockingQueue<>(2);
        UseUtils.set(e -> {
            try {
                blockingQueue.put(e);
                UseUtils.printThread("put", true, e);
            } catch (InterruptedException ex) {
                UseUtils.printThread("put", false, "", "异常");
            }
        });
        UseUtils.get(() -> {
            try {
                Integer take = blockingQueue.take();
                UseUtils.printThread("take", true, take);
                return take;
            } catch (InterruptedException ex) {
                UseUtils.printThread("take", false, "", "异常");
            }
            return null;
        });
    }
}
