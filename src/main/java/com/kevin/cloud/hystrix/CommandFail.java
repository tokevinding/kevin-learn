package com.kevin.cloud.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

/**
 * @author dinghaifeng
 * @date 2020-11-30 13:45:24
 * @desc
 */
public class CommandFail extends HystrixCommand<String> {

    private final String name;

    public CommandFail(String name) {
        super(HystrixCommandGroupKey.Factory.asKey("KevinGroup-Command"));
        this.name = name;
    }

    @Override
    protected String run() {
        throw new RuntimeException("this command always fails");
    }

    @Override
    protected String getFallback() {
        return "Kevin Failure " + name + "!";
    }

    private static void sleep(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void println(String content) {
        System.out.println(content);
    }
}
