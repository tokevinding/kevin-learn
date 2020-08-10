package com.kevin.design.create.factory;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KDoor {

    public static void main(String[] args) {
        KLoggerFactory loggerFactory = new KLoggerFactory();
        loggerFactory.getLogger(KLoggerEnum.K_SLF4J_LOGGER).log("日志内容");
        loggerFactory.getLogger(KLoggerEnum.K_LOG4J_LOGGER).log("日志内容");
    }
}
