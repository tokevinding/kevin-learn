package com.kevin.threads.juc.cas;

import lombok.Getter;
import lombok.Setter;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author Kevin
 * @date 2020-09-13 22:20:17
 * @desc
 */
public class UseUnsafe {
    private static final Unsafe unsafe;
    private static final long valueOffset;
    @Getter
    @Setter
    private volatile int unsafeValue;

    public UseUnsafe() {
        this.unsafeValue = 0;
    }
    public UseUnsafe(int unsafeValue) {
        this.unsafeValue = unsafeValue;
    }

    static {
        try {
            //获取成员变量
            Field field=Unsafe.class.getDeclaredField("theUnsafe");
            //设置为可访问
            field.setAccessible(true);
            //是静态字段,用null来获取Unsafe实例
            unsafe=(Unsafe)field.get(null);

            //获取本对象的线程安全字段（作偏移量）
            valueOffset = unsafe.objectFieldOffset(UseUnsafe.class.getDeclaredField("unsafeValue"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    public final boolean tryIncrement() {
        return compareAndSet(unsafe.getIntVolatile(this, valueOffset), unsafe.getIntVolatile(this, valueOffset) + 1);
    }

    public final void add(int addValue) {
        unsafe.getAndAddInt(this, valueOffset, addValue);
    }

    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }
}
