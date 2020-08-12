package com.kevin.design.structure.proxy.cglib;

import com.kevin.tools.utils.ConsoleOutputUtils;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class CglibTarget {
    public void targetMethod() {
        ConsoleOutputUtils.println("I am cglib target method!!");
    }
}