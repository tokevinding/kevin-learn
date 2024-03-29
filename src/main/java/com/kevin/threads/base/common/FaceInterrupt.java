package com.kevin.threads.base.common;

/**
 * @author kevin
 * @date 2021-08-31 17:36:20
 * @desc
 */
public class FaceInterrupt {
    public static void main(String[] args) {
        FaceInterrupt lock = new FaceInterrupt();
        new Thread(() -> {
            Thread.currentThread().interrupt();
            synchronized (lock) {
                try {
//                Thread.sleep(10000);
//                    lock.wait();
                  Thread.currentThread().join();
//                    LockSupport.park();//不抛异常
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("抛出InterruptedException异常");
                }
                System.out.println("over");
            }
        }).start();
    }
}
