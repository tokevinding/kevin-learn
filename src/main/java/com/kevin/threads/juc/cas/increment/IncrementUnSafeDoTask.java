package com.kevin.threads.juc.cas.increment;

import com.kevin.threads.juc.cas.DoTask;
import com.kevin.threads.juc.cas.UseUnsafe;
import com.kevin.tools.annotation.ThreadNoSafe;
import lombok.AllArgsConstructor;

/**
 * @author Kevin
 * @date 2020-09-13 23:27:04
 * @desc
 */
@ThreadNoSafe
@AllArgsConstructor
public class IncrementUnSafeDoTask implements DoTask {
    private UseUnsafe faceUnsafe;
    /**
     * 执行任务
     */
    @Override
    public void doTask() {
        for (int j = 0; j < 10000; j++) {
            while (!unsafeIncrement(faceUnsafe)) {
                System.out.println("更新失败");
            }
        }
    }

    private boolean unsafeIncrement(UseUnsafe faceUnsafe) {
        faceUnsafe.setUnsafeValue(faceUnsafe.getUnsafeValue() + 1);
        return true;
    }
}
