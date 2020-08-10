package com.kevin.design.create.singleton.enums;

import com.kevin.tools.annotation.ThreadSafe;

/**
 * @author kevin
 * @date 2020-08-09 22:30:47
 * @desc
 */
@ThreadSafe
public enum SingleObject {
    INSTANCE;
    public void showMessage() {
        System.out.println("enum create SingleObject showMessage!");
    }
}
