package com.kevin.design.structure.proxy.cglib.technical;

import com.kevin.design.structure.proxy.cglib.technical.clazz.SampleBean;
import com.kevin.design.structure.proxy.cglib.technical.clazz.SampleInterface;
import net.sf.cglib.reflect.MethodDelegate;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class MethodDelegateTest {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException {
        SampleBean bean = new SampleBean();
        bean.setValue("simple value");
        SampleInterface getValue = (SampleInterface)MethodDelegate.create(bean, "getValue", SampleInterface.class);
        System.out.println(getValue.simpleMethod());
    }
}