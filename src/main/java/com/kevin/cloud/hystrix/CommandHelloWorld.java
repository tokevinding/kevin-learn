package com.kevin.cloud.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

/**
 * @author dinghaifeng
 * @date 2020-11-30 13:45:24
 * @desc
 */
public class CommandHelloWorld extends HystrixCommand<String> {

    private final String name;

    public CommandHelloWorld(String name) {
        super(HystrixCommandGroupKey.Factory.asKey("KevinGroup-Command"));
        this.name = name;
    }

    @Override
    protected String run() {
        // 一个真实的例子就像这里的网络调用一样
        return "Hello " + name + "!";
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
