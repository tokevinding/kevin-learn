package com.kevin.spring.aop.proxy.advice;

import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

/**
 * @author Kevin
 * @date 2020-08-31 13:18:34
 * @desc
 */
public class FaceMethodBeforeAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        ConsoleOutputUtils.println("++前置增强++");
    }
}
