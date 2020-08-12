package com.kevin.design.structure.proxy.cglib;

/**
 * @author kevin
 * @date 2020-08-11 20:10:54
 * @desc
 */
public class KDoor {
    public static void main(String[] args) {
        CglibTarget target = new ProxyFactory(new CglibProxy()).getInstance(new CglibTarget());
        target.targetMethod();
    }
}
