package com.kevin.design.structure.proxy.cglib;

import com.kevin.tools.utils.ConsoleOutputUtils;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class CglibProxy implements MethodInterceptor {

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        ConsoleOutputUtils.println("cglib do before");
        ConsoleOutputUtils.hr();

        Object ret = methodProxy.invokeSuper(o, objects);

        ConsoleOutputUtils.hr();
        ConsoleOutputUtils.println("cglib do after");
        return ret;
    }
}
