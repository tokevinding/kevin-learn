package com.kevin.spring.aop.aspectJ;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

import java.util.Optional;

/**
 * @author Kevin
 * @date 2020-08-12 11:14:57
 * @desc
 */
@Aspect
public class AspectjAopTwo {

    @Pointcut("execution(* com.kevin.design.structure.proxy.jdk.*.*(..))")
    public void pointcut2() {}

    /**
     * 前置通知 	在一个方法执行之前，执行通知。
     */
    @Before("pointcut2()")
    public void beforeAdvice2(){
        System.out.println("时机2: beforeAdvice");
    }
    /**
     * 后置通知 	在一个方法执行之后，不考虑其结果（是否异常），执行通知。
     */
    @After("pointcut2()")
    public void afterAdvice2(){
        System.out.println("时机2: afterAdvice");
    }
    /**
     * 返回后通知    在一个方法执行之后，只有在方法成功完成时（异常时不执行），才能执行通知。
     */
    @AfterReturning(pointcut = "pointcut2()", returning = "retVal")
    public void afterReturningAdvice2(Object retVal){
        System.out.println("时机2: afterReturningAdvice Returning:"
                + Optional.ofNullable(retVal).map(Object::toString).orElse(null));
    }
    /**
     * 抛出异常后通知  在一个方法执行之后，只有在方法退出抛出异常时，才能执行通知。
     */
    @AfterThrowing(pointcut = "pointcut2()", throwing = "ex")
    public void afterThrowingAdvice2(Exception ex){
        System.out.println("时机2: afterThrowingAdvice" + ex.toString());
    }
    /**
     * 环绕方法
     */
    @Around("pointcut2()")
    public Object around2(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("时机2: around before");
        Object result = joinPoint.proceed();
        System.out.println("时机2: around after");
        return result;
    }

}
