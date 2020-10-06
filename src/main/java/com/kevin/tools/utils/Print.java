package com.kevin.tools.utils;

import java.util.Arrays;

/**
 * @ClassName: Print
 * @Author: 丁海峰
 * @Time: 2020-05-10 11:02
 * @description:
 **/
public class Print {

    public static void print(Object ... params) {
        Arrays.stream(params).forEach(Print::print);
    }

    public static void println(Object ... params) {
        Arrays.stream(params).forEach(Print::println);
    }

    /********************************************基础型打印****************************************/
    public static void printfln(String model, Object ... params) {
        System.out.printf(model, params);
        System.out.println();
    }

    public static void println(Object param) {
        System.out.println(param);
    }

    public static void printlns(Object param, int lns) {
        System.out.println(param);
        for (int i = 0; i < lns; i++) {
            System.out.println();
        }
    }

    public static void print(Object param) {
        System.out.print(param);
    }



}
