package com.kevin.design.create.singleton.hungry;

import com.kevin.tools.annotation.ThreadSafe;

/**
 * @author kevin
 * @date 2020-08-09 22:30:47
 * @desc
 */
@ThreadSafe
public class SingleObject {

    /**
     * 创建 SingleObject 的一个对象
     */
    private static final SingleObject INSTANCE = new SingleObject();

    /**
     * 让构造函数为 private，这样该类就不会被实例化
     */

    private SingleObject() {
    }

    /**
     * 获取唯一可用的对象
     */
    public static SingleObject getInstance() {
        return INSTANCE;
    }

    public void showMessage() {
        System.out.println("hungry create SingleObject showMessage!");
    }
}
