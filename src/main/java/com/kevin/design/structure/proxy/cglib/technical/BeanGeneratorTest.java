package com.kevin.design.structure.proxy.cglib.technical;

import net.sf.cglib.beans.BeanGenerator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class BeanGeneratorTest {

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        BeanGenerator generator = new BeanGenerator();
        generator.addProperty("value",String.class);
        Object myBean = generator.create();
        Method setter = myBean.getClass().getMethod("setValue",String.class);
        setter.invoke(myBean,"Hello cglib");
        Method getter = myBean.getClass().getMethod("getValue");
        System.out.println("Hello cglib".equals(getter.invoke(myBean)));
    }
}