package com.kevin.threads.juc.cas;

import com.kevin.threads.juc.cas.add.AddSafeDoTask;
import com.kevin.threads.juc.cas.add.AddUnSafeDoTask;
import com.kevin.threads.juc.cas.increment.IncrementSafeDoTask;
import com.kevin.tools.utils.ThreadUtils;

/**
 * @author Kevin
 * @date 2020-09-13 22:20:17
 * @desc
 */
public class FaceUnsafeHome {

    public static void main(String[] args) {
        testCASToAdd();
    }

    /**
     * 测试Cas的Add
     */
    public static void testCASToAdd() {
        UseUnsafe useUnsafe = new UseUnsafe(0);
        ThreadUtils.multiThreadProcess(new AddSafeDoTask(useUnsafe));
//        ThreadUtils.multiThreadProcess(new AddUnSafeDoTask(useUnsafe));
        System.out.println(useUnsafe.getUnsafeValue());
    }

    /**
     * 测试Cas的Increment
     */
    public static void testCASToIncrement() {
        UseUnsafe useUnsafe = new UseUnsafe(0);
        ThreadUtils.multiThreadProcess(new IncrementSafeDoTask(useUnsafe));
//        ThreadUtils.multiThreadProcess(new IncrementUnSafeDoTask(useUnsafe));
        System.out.println(useUnsafe.getUnsafeValue());
    }
}
