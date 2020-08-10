package com.kevin.design.create.singleton.innerClass;

import com.kevin.tools.annotation.ThreadSafe;

/**
 * @author kevin
 * @date 2020-08-09 22:30:47
 * @desc
 */
@ThreadSafe
public class SingleObject {
    /**
     * 让构造函数为 private，这样该类就不会被实例化
     */
    private SingleObject() {
    }

    /**
     * 获取唯一可用的对象
     */
    public static SingleObject getInstance() {
        return SingleHolder.instance;
    }

    public void showMessage() {
        System.out.println("inner class create SingleObject showMessage!");
    }

    private static class SingleHolder {
        private static final SingleObject instance = new SingleObject();
    }
}
