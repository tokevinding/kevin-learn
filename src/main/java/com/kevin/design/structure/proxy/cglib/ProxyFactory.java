package com.kevin.design.structure.proxy.cglib;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class ProxyFactory {

    private Object target;

    private Callback callback;

    public ProxyFactory(Callback callback) {
        this.callback = callback;
    }

    public  <T> T getInstance(T target) {
        this.target = target;
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(this.target.getClass());
        // 回调方法
        enhancer.setCallback(callback);
        // 创建代理对象
        return (T)enhancer.create();
    }
}
