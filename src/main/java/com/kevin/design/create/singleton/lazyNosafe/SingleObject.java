package com.kevin.design.create.singleton.lazyNosafe;

import com.kevin.tools.annotation.ThreadNoSafe;

import java.util.Objects;

/**
 * @author kevin
 * @date 2020-08-09 22:30:47
 * @desc
 */
@ThreadNoSafe
public class SingleObject {

    /**
     * 创建 SingleObject 的一个对象
     */
    private static SingleObject instance;

    /**
     * 让构造函数为 private，这样该类就不会被实例化
     */

    private SingleObject() {
    }

    /**
     * 获取唯一可用的对象
     */
    public static synchronized SingleObject getInstance() {
        if (Objects.nonNull(instance)) {
            return instance;
        }
        return (instance = new SingleObject());
    }

    public void showMessage() {
        System.out.println("Lazy no safe SingleObject showMessage!");
    }
}
