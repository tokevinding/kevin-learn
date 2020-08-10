package com.kevin.design.create.factory;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KSlf4jLogger implements KLogger {
    @Override
    public void log(String content) {
        System.out.println("Slf4j log:" + content);
    }
}
