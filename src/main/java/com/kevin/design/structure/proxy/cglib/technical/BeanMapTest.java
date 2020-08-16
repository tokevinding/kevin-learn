package com.kevin.design.structure.proxy.cglib.technical;

import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.beans.BeanMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class BeanMapTest {

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        BeanGenerator generator = new BeanGenerator();
        generator.addProperty("username",String.class);
        generator.addProperty("password",String.class);
        Object bean = generator.create();
        Method setUsername = bean.getClass().getMethod("setUsername", String.class);
        Method setPassword = bean.getClass().getMethod("setPassword", String.class);
        setUsername.invoke(bean, "kevin");
        setPassword.invoke(bean, "123");

        BeanMap beanMap = BeanMap.create(bean);

        System.out.println(beanMap.get("username"));
        System.out.println(beanMap.get("password"));
    }
}