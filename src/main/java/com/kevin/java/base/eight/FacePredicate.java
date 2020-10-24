package com.kevin.java.base.eight;

import com.kevin.tools.utils.ConsoleOutputUtils;
import lombok.ToString;

import java.util.function.Predicate;

/**
 * @author dinghaifeng
 * @date 2020-10-10 13:13:13
 * @desc
 */
@ToString
public class FacePredicate {
    public static void main(String[] args) {
        facePredicate();
    }

    private static void facePredicate() {
        int a = 10;
        //创建
        Predicate<Integer> p1 = (item -> item.compareTo(a) > 0);
        //使用test
        System.out.println(p1.test(a));
        System.out.println(p1.test(a+1));
        ConsoleOutputUtils.hr();

        //取反
        Predicate<Integer> p2 = p1.negate();
        System.out.println(p2.test(a));
        ConsoleOutputUtils.hr();

        //取与
        System.out.println(p1.and(p2).test(a));

        ConsoleOutputUtils.hr();
        //取或
        System.out.println(p1.or(p2).test(a));
    }

    private String name = "FacePredicate";
}
