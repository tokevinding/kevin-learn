package com.kevin.threads.juc.cas;

import com.kevin.threads.juc.cas.add.AddSafeDoTask;
import com.kevin.threads.juc.cas.add.AddUnSafeDoTask;
import com.kevin.threads.juc.cas.increment.IncrementSafeDoTask;
import com.kevin.threads.juc.cas.increment.IncrementUnSafeDoTask;
import com.kevin.tools.utils.ThreadUtils;

/**
 * @author Kevin
 * @date 2020-09-13 22:20:17
 * @desc
 */
public class FaceUnsafeHome {

    public static void main(String[] args) {
        testCASToIncrement();
        testCASToAdd();
    }

    /**
     * 测试Cas的Add
     */
    public static void testCASToAdd() {
        UseUnsafe useUnsafe1 = new UseUnsafe();
        UseUnsafe useUnsafe2 = new UseUnsafe();
        ThreadUtils.multiThreadProcess(new AddSafeDoTask(useUnsafe1));
        ThreadUtils.multiThreadProcess(new AddUnSafeDoTask(useUnsafe2));
        System.out.println("safe add - value: " + useUnsafe1.getUnsafeValue());
        System.out.println("unsafe add - value: " + useUnsafe2.getUnsafeValue());
    }

    /**
     * 测试Cas的Increment
     */
    public static void testCASToIncrement() {
        UseUnsafe useUnsafe1 = new UseUnsafe();
        UseUnsafe useUnsafe2 = new UseUnsafe();
        ThreadUtils.multiThreadProcess(new IncrementSafeDoTask(useUnsafe1));
        ThreadUtils.multiThreadProcess(new IncrementUnSafeDoTask(useUnsafe2));
        System.out.println("safe Increment - value: " + useUnsafe1.getUnsafeValue());
        System.out.println("unsafe Increment - value: " + useUnsafe2.getUnsafeValue());
    }
}
