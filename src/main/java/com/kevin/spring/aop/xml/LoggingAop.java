package com.kevin.spring.aop.xml;

import java.util.Optional;

/**
 * @author dinghaifeng
 * @date 2020-08-12 11:14:57
 * @desc
 */
public class LoggingAop {
    /**
     * 前置通知 	在一个方法执行之前，执行通知。
     */
    public void beforeAdvice(){
        System.out.println("时机: beforeAdvice");
    }
    /**
     * 后置通知 	在一个方法执行之后，不考虑其结果（是否异常），执行通知。
     */
    public void afterAdvice(){
        System.out.println("时机: afterAdvice");
    }
    /**
     * 返回后通知    在一个方法执行之后，只有在方法成功完成时（异常时不执行），才能执行通知。
     */
    public void afterReturningAdvice(Object retVal){
        System.out.println("时机: afterReturningAdvice Returning:"
                + Optional.ofNullable(retVal).map(Object::toString).orElse(null));
    }
    /**
     * 抛出异常后通知  在一个方法执行之后，只有在方法退出抛出异常时，才能执行通知。
     */
    public void afterThrowingAdvice(Exception ex){
        System.out.println("时机: afterThrowingAdvice" + ex.toString());
    }
}
