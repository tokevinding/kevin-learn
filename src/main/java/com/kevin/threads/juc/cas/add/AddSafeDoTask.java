package com.kevin.threads.juc.cas.add;

import com.kevin.threads.juc.cas.DoTask;
import com.kevin.threads.juc.cas.UseUnsafe;
import lombok.AllArgsConstructor;

/**
 * @author Kevin
 * @date 2020-09-13 23:27:04
 * @desc
 */
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
