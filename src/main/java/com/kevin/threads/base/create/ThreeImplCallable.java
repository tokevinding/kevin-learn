package com.kevin.threads.base.create;

import com.kevin.tools.utils.ConsoleOutputUtils;

import java.util.concurrent.Callable;

/**
 * @author Kevin
 * @date 2020-09-11 18:48:14
 * @desc
 */
public class ThreeImplCallable implements Callable<String> {
    @Override
    public String call() throws Exception {
        ConsoleOutputUtils.hr("ThreeImplCallable 睡2s！");
        Thread.sleep(2000);
        ConsoleOutputUtils.hr("ThreeImplCallable 醒了！");
        return "success";
    }
}
