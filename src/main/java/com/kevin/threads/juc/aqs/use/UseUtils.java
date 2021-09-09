package com.kevin.threads.juc.aqs.use;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author kevin
 * @date 2021-08-20 11:28:23
 * @desc
 */
public class UseUtils {

    public static void set(Consumer<Integer> c) {
        new Thread(() -> {
            try {
                for (int i = 1; i < 10; i++) {
                    c.accept(i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void get(Supplier<Integer> get) {
        new Thread(() -> {
            try {
                for (int i = 1; i < 10; i++) {
                    get.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void setMultiThread(Consumer<Integer> c) {
        for (int i = 1; i < 10; i++) {
            AtomicInteger e = new AtomicInteger(i);
            new Thread(() -> {
                try {
                    c.accept(e.get());
                    TimeUnit.SECONDS.sleep(2);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    public static void getMultiThread(Supplier<Integer> get) {
        for (int i = 1; i < 10; i++) {
            new Thread(() -> {
                try {
                    get.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public static void getMultiThreadCurrentStart(Supplier<Integer> get) {
        List<Thread> ts = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            ts.add(new Thread(() -> {
                try {
                    get.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }
        for (Thread t : ts) {
            t.start();
        }
    }

    public static void printThread(String operate, boolean success, Object o) {
        printThread(operate, success, o, "");
    }

    public static void printThread(String operate, boolean success, Object o, String append) {
        System.out.println(append + " " + o + " - " + operate + (success ? "成功" : "失败") + ", Thread Name: " + Thread.currentThread().getName());
    }
}
