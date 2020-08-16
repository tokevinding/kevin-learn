package com.kevin.design.structure.proxy.cglib.technical;

import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.InterfaceMaker;
import net.sf.cglib.util.StringSwitcher;
import org.objectweb.asm.Type;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class InterfaceMakerTest {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException {
        Signature signature = new Signature("kevinMethod", Type.DOUBLE_TYPE, new Type[]{Type.INT_TYPE});
        InterfaceMaker maker = new InterfaceMaker();
        maker.add(signature, new Type[0]);
        Class clazz = maker.create();
        System.out.println(clazz.getMethods().length);
        System.out.println(clazz.getMethods()[0].getName());
        //下面会抛出异常， 因为没有newInstance方法
        Object newInstance = clazz.newInstance();
        System.out.println(newInstance);
    }
}