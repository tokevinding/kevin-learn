package com.kevin.threads.base.create;

import com.kevin.tools.utils.ConsoleOutputUtils;
import lombok.SneakyThrows;

/**
 * @author Kevin
 * @date 2020-09-11 18:48:14
 * @desc
 */
public class OneExtendThread extends Thread {

    @SneakyThrows
    @Override
    public void run() {
        ConsoleOutputUtils.hr("OneExtendThread 睡2s！");
        Thread.sleep(2000);
        ConsoleOutputUtils.hr("OneExtendThread 醒了！");
    }
}
