package com.kevin.design.structure.proxy.cglib.technical;

import com.kevin.design.structure.proxy.cglib.technical.clazz.SampleBean;
import com.kevin.design.structure.proxy.cglib.technical.clazz.SampleBeanConstructorDelegate;
import net.sf.cglib.reflect.ConstructorDelegate;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class ConstructorDelegateTest {
    public static void main(String[] args) {
        SampleBeanConstructorDelegate constructorDelegate = (SampleBeanConstructorDelegate)ConstructorDelegate.create(SampleBean.class, SampleBeanConstructorDelegate.class);
        SampleBean newInstance = (SampleBean)constructorDelegate.newInstance("constructor args");
        System.out.println(SampleBean.class.isAssignableFrom(newInstance.getClass()));
        System.out.println(newInstance.getValue());
    }
}