package com.kevin.threads.juc.cas.increment;

import com.kevin.threads.juc.cas.DoTask;
import com.kevin.threads.juc.cas.UseUnsafe;
import com.kevin.tools.annotation.ThreadSafe;
import lombok.AllArgsConstructor;

/**
 * @author Kevin
 * @date 2020-09-13 23:27:04
 * @desc
 */
@ThreadSafe
@AllArgsConstructor
public class IncrementSafeDoTask implements DoTask {
    private UseUnsafe faceUnsafe;
    /**
     * 执行任务
     */
    @Override
    public void doTask() {
        for (int j = 0; j < 10000; j++) {
            //真正执行的CAS
            while (!safeIncrement(faceUnsafe)) {
                System.out.println("CAS 更新失败!!");
            }
        }
    }

    private boolean safeIncrement(UseUnsafe faceUnsafe) {
        return faceUnsafe.tryIncrement();
    }
}
