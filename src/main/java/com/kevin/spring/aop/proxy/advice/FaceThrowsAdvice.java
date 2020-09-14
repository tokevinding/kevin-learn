package com.kevin.spring.aop.proxy.advice;

import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.aop.ThrowsAdvice;

import java.lang.reflect.Method;

/**
 * @author Kevin
 * @date 2020-08-31 13:15:23
 * @desc
 */
public class FaceThrowsAdvice implements ThrowsAdvice {
    //注意方法名称必须为afterThrowing
    public void afterThrowing(Method method, Object[] args, Object target, Exception e) {
        ConsoleOutputUtils.println("--后置异常处理增强1--");
    }

    public void afterThrowing(Exception e) {
        ConsoleOutputUtils.println("--后置异常处理增强2--");
    }
}
