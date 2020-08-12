package com.kevin.design.structure.proxy.jdk;

import com.kevin.tools.utils.ConsoleOutputUtils;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class TargetImpl implements Target {

    @Override
    public void targetMethod() {
        ConsoleOutputUtils.println("I am target method!!");
    }
}
