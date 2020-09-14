package com.kevin.spring.aop.proxy.advice;

import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.aop.AfterReturningAdvice;

import java.lang.reflect.Method;

/**
 * @author Kevin
 * @date 2020-08-31 13:17:31
 * @desc
 */
public class FaceAfterReturningAdvice implements AfterReturningAdvice {
    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        ConsoleOutputUtils.println("--后置增强, returnValue: %s--", returnValue);
    }
}
