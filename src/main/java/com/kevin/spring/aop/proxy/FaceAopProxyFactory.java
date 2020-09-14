package com.kevin.spring.aop.proxy;

import com.kevin.spring.aop.body.ToLogProgram;
import com.kevin.spring.aop.proxy.advice.FaceAfterReturningAdvice;
import com.kevin.spring.aop.proxy.advice.FaceMethodBeforeAdvice;
import com.kevin.spring.aop.proxy.advice.FaceThrowsAdvice;
import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;

/**
 * @author Kevin
 * @date 2020-08-31 18:59:32
 * @desc
 */
public class FaceAopProxyFactory {
    public static void main(String[] args) {
        DefaultAopProxyFactory aopProxyFactory = new DefaultAopProxyFactory();
        AdvisedSupport support = new AdvisedSupport();
        support.setTarget(new ToLogProgram());
        // 1.添加前置增强
        support.addAdvice(new FaceMethodBeforeAdvice());
        // 2.添加后置增强
        support.addAdvice(new FaceAfterReturningAdvice());
        // 3.后置异常处理增强
        support.addAdvice(new FaceThrowsAdvice());
        AopProxy aopProxy = aopProxyFactory.createAopProxy(support);

        ToLogProgram logProgram = (ToLogProgram) aopProxy.getProxy();
        logProgram.toLogReturnMethod();
        ConsoleOutputUtils.hr();
        logProgram.toLogErrorMethod();
    }
}
