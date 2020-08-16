package com.kevin.design.structure.proxy.cglib.technical;

import net.sf.cglib.proxy.Mixin;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class MixinTest {
    interface Interface1{
        String first();
    }
    interface Interface2{
        String second();
    }

    static class Class1 implements Interface1{
        @Override
        public String first() {
            return "first";
        }
    }

    static class Class2 implements Interface2{
        @Override
        public String second() {
            return "second";
        }
    }

    interface MixinInterface extends Interface1, Interface2{

    }

    public static void main(String[] args) {
        Mixin mixin = Mixin.create(new Class[]{Interface1.class, Interface2.class,
                MixinInterface.class}, new Object[]{new Class1(),new Class2()});
        MixinInterface mixinDelegate = (MixinInterface) mixin;
        System.out.println("first".equals(mixinDelegate.first()));
        System.out.println("second".equals(mixinDelegate.second()));
    }
}