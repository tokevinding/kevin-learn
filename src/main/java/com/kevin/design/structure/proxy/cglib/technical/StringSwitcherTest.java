package com.kevin.design.structure.proxy.cglib.technical;

import net.sf.cglib.util.StringSwitcher;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class StringSwitcherTest {
    public static void main(String[] args) {
        String[] names = {"a", "b"};
        int[] ages = {97, 98};
        StringSwitcher switcher = StringSwitcher.create(names, ages, true);
        System.out.println(switcher.intValue("a"));
        System.out.println(switcher.intValue("b"));
    }
}