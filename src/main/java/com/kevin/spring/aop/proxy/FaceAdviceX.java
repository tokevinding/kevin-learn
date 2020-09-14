package com.kevin.spring.aop.proxy;

import com.kevin.spring.aop.body.ToLogProgram;
import com.kevin.spring.aop.proxy.advice.FaceAfterReturningAdvice;
import com.kevin.spring.aop.proxy.advice.FaceMethodBeforeAdvice;
import com.kevin.spring.aop.proxy.advice.FaceThrowsAdvice;
import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.reflect.Method;

/**
 * Advised、Advice、Advisor的区别
 *
 * Advised:   包含所有的Advisor 和 Advice
 * Advice:    通知拦截器
 * Advisor:   通知 + 切入点的适配器
 */
public class FaceAdviceX {
    public static void main(String[] args) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(new ToLogProgram());
        ToLogProgram logProgram = (ToLogProgram) proxyFactory.getProxy();
        Advised advised = (Advised) logProgram;
        // 1.添加前置增强
        advised.addAdvice(new FaceMethodBeforeAdvice());
        // 2.添加后置增强
        advised.addAdvice(new FaceAfterReturningAdvice());
        // 3.后置异常处理增强
        advised.addAdvice(new FaceThrowsAdvice());

        logProgram.toLogReturnMethod();
        ConsoleOutputUtils.hr();
        logProgram.toLogErrorMethod();
    }
}
