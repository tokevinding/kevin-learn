package com.kevin.design.create.factory;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KLoggerFactory {

    public KLogger getLogger(KLoggerEnum loggerEnum) {
        if(loggerEnum == null){
            return null;
        }
        if(loggerEnum.equals(KLoggerEnum.K_SLF4J_LOGGER)){
            return new KSlf4jLogger();
        }
        if(loggerEnum.equals(KLoggerEnum.K_LOG4J_LOGGER)){
            return new KLog4jLogger();
        }
        return null;
    }
}
