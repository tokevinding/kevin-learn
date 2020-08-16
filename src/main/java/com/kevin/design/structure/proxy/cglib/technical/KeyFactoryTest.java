package com.kevin.design.structure.proxy.cglib.technical;

import com.kevin.design.structure.proxy.cglib.technical.clazz.SampleKeyFactory;
import net.sf.cglib.core.KeyFactory;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class KeyFactoryTest {

    public static void main(String[] args) {
        SampleKeyFactory keyFactory = (SampleKeyFactory)KeyFactory.create(SampleKeyFactory.class);
        Object num1 = keyFactory.newInstance("num", 1);
        Object num2 = keyFactory.newInstance("num", 1);
        System.out.println(num1.equals(num2));
    }
}