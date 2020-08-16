package com.kevin.design.structure.proxy.cglib.technical;

import com.kevin.design.structure.proxy.cglib.technical.clazz.MulticastInterface;
import com.kevin.design.structure.proxy.cglib.technical.clazz.SimpleMulticastBean;
import net.sf.cglib.reflect.MulticastDelegate;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class MulticastBeanTest {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException {
        MulticastDelegate multicastDelegate = MulticastDelegate.create(MulticastInterface.class);
//        SimpleMulticastBean first = new SimpleMulticastBean("first");
//        SimpleMulticastBean second = new SimpleMulticastBean("second");
        SimpleMulticastBean first = new SimpleMulticastBean();
        SimpleMulticastBean second = new SimpleMulticastBean();

        multicastDelegate = multicastDelegate.add(first);
        multicastDelegate = multicastDelegate.add(second);

        MulticastInterface multicastInterface = (MulticastInterface) multicastDelegate;
        multicastInterface.setName("multicast1 name");
        System.out.println(first.getName());
        System.out.println(second.getName());
    }
}