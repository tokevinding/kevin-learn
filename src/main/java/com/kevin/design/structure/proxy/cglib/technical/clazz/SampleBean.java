package com.kevin.design.structure.proxy.cglib.technical.clazz;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class SampleBean {

    public SampleBean() {
    }

    public SampleBean(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}