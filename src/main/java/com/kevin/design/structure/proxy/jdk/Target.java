package com.kevin.design.structure.proxy.jdk;

import com.kevin.tools.annotation.ThreadNoSafe;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public interface Target {
    @ThreadNoSafe
    String targetMethod();
}
