package com.kevin.design.structure.proxy.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class ProxyFactory {
    public static <T> T getProxy(Class<T> interfaceClass, InvocationHandler proxy) {
        return (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, proxy);
    }
}
