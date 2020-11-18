package com.kevin.threads.juc.unsafe;

/**
 * @author dinghaifeng
 * @date 2020-10-24 13:44:58
 * 分析：
 * 1.Unsafe可认为是Java中留下的后门，提供了一些低层次操作，如直接内存访问、线程调度等
 */
public class FaceUnSafe {



    public interface NativeUnsafe {

    }

    /**
     * 原始方法：public final native boolean compareAndSwapInt(Object var1, long var2, int var4, int var5);
     *
     * 说明：
     * 1.这个是一个native方法， 第一个参数为需要改变的对象，第二个为偏移量(即之前求出来的headOffset的值)，第三个参数为期待的值，第四个为更新后的值
     * 2.整个方法的作用是如果当前时刻的值等于预期值var4相等，则更新为新的期望值 var5，如果更新成功，则返回true，否则返回false；
     * @param var1
     * @param var2
     * @param var4
     * @param var5
     * @return
     */
    public static boolean compareAndSwapInt(Object var1, long var2, int var4, int var5) {
        return true;
    }
}
