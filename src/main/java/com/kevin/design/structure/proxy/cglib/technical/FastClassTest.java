package com.kevin.design.structure.proxy.cglib.technical;

import com.kevin.design.structure.proxy.cglib.technical.clazz.SampleBean;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class FastClassTest {
    public static void main(String[] args) throws InvocationTargetException {
        FastClass fastClass = FastClass.create(SampleBean.class);
        FastMethod fastMethod = fastClass.getMethod("getValue",new Class[0]);
        SampleBean bean = new SampleBean();
        bean.setValue("Hello world");
        System.out.println("Hello world".equals(fastMethod.invoke(bean, new Object[0])));
    }
}