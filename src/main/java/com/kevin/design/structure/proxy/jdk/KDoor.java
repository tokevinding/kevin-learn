package com.kevin.design.structure.proxy.jdk;

/**
 * @author kevin
 * @date 2020-08-11 20:10:54
 * @desc
 */
public class KDoor {
    public static void main(String[] args) {
        Target proxy = ProxyFactory.getProxy(Target.class, new TargetProxy(new TargetImpl()));
        proxy.targetMethod();
    }
}
