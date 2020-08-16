package com.kevin.design.structure.proxy.cglib.technical;

import com.kevin.tools.utils.ConsoleOutputUtils;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.FixedValue;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class EnhancerTest {
    public void method() {
        //没有执行
        ConsoleOutputUtils.println("I am cglib target method!!");
    }

    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(EnhancerTest.class);
        enhancer.setCallback((FixedValue) () -> "Hello cglib");
        EnhancerTest proxy = (EnhancerTest) enhancer.create();
        proxy.method();
        System.out.println(proxy.toString());
        System.out.println(proxy.getClass());
        System.out.println(proxy.hashCode());
    }
}