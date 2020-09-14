package com.kevin.design.structure.proxy.jdk.interf;

import com.kevin.design.structure.proxy.jdk.ProxyFactory;
import com.kevin.design.structure.proxy.jdk.Target;
import com.kevin.design.structure.proxy.jdk.TargetImpl;
import com.kevin.design.structure.proxy.jdk.TargetProxy;
import com.kevin.spring.aop.body.ToLogProgram;
import com.kevin.spring.aop.proxy.advice.FaceAfterReturningAdvice;
import com.kevin.spring.aop.proxy.advice.FaceMethodBeforeAdvice;
import com.kevin.spring.aop.proxy.advice.FaceThrowsAdvice;
import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.aop.framework.Advised;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author kevin
 * @date 2020-08-11 20:10:54
 * @desc
 */
public class BaseInvocationHandler implements InvocationHandler {

    public static void main(String[] args) {
        Target proxy = ProxyFactory.getProxy(Target.class, new BaseInvocationHandler());
        ConsoleOutputUtils.hr("1级代理");
        System.out.println(proxy.targetMethod());

        org.springframework.aop.framework.ProxyFactory proxyFactory = new org.springframework.aop.framework.ProxyFactory();
        proxyFactory.setTarget(proxy);
        //二次代理
        Target proxy2 = (Target) proxyFactory.getProxy();
        Advised advised = (Advised) proxy2;
        // 1.添加前置增强
        advised.addAdvice(new FaceMethodBeforeAdvice());
        // 2.添加后置增强
        advised.addAdvice(new FaceAfterReturningAdvice());
        // 3.后置异常处理增强
        advised.addAdvice(new FaceThrowsAdvice());

        ConsoleOutputUtils.hr("2级代理");
        System.out.println(proxy2.targetMethod());
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ConsoleOutputUtils.println("I just do something what i want not about this method!");
        return "invoked result";
    }
}
