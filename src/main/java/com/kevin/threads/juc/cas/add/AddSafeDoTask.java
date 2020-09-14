package com.kevin.threads.juc.cas.add;

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
public class AddSafeDoTask implements DoTask {
    private UseUnsafe faceUnsafe;
    /**
     * 执行任务
     */
    @Override
    public void doTask() {
        for (int j = 0; j < 10000; j++) {
            faceUnsafe.add(j);
        }
    }
}
