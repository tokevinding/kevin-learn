package com.kevin.design.create.factory;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KLog4jLogger implements KLogger {
    @Override
    public void log(String content) {
        System.out.println("Log4j log:" + content);
    }
}
