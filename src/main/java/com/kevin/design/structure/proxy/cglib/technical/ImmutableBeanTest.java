package com.kevin.design.structure.proxy.cglib.technical;

import net.sf.cglib.beans.ImmutableBean;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class ImmutableBeanTest {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static void main(String[] args) {
        ImmutableBeanTest target = new ImmutableBeanTest();
        target.setName("目标直接修改值");
        System.out.println("level 1 set over");
        ImmutableBeanTest immutabled = (ImmutableBeanTest)ImmutableBean.create(target);
        immutabled.setName("不允许修改！！");
        System.out.println("level 2 set over");
    }
}