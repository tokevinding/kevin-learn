package com.kevin.threads.base.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author dinghaifeng
 * @date 2021-08-31 17:36:20
 * @desc
 */
public class FacePark {
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
//            Thread.currentThread().interrupt();
            LockSupport.park();
            System.out.println("park完成");
        });
        t1.start();
        System.out.println("主流程完成");
    }
}
