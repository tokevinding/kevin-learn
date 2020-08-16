package com.kevin.design.structure.proxy.cglib.technical.clazz;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class SimpleMulticastBean implements MulticastInterface {
    private String name;

    public SimpleMulticastBean() {
    }

    public SimpleMulticastBean(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}