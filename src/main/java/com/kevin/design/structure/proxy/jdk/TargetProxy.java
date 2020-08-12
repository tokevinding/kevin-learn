package com.kevin.design.structure.proxy.jdk;

import com.kevin.tools.utils.ConsoleOutputUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class TargetProxy implements InvocationHandler {

    private Object target;

    public TargetProxy(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ConsoleOutputUtils.println("do before");
        ConsoleOutputUtils.hr();
        Object result = method.invoke(target, args);
        ConsoleOutputUtils.hr();
        ConsoleOutputUtils.println("do after");
        return result;
    }
}
