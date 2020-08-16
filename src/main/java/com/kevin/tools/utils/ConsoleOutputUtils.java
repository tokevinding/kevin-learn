package com.kevin.tools.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @ClassName: ConsoleOutput
 * @Author: kevin
 * @Time: 2020-03-20 20:06
 * @description: 内容输出
 **/
@Slf4j
public class ConsoleOutputUtils {
    public static void println(String content) {
        System.out.println(content);
    }
    public static void hr() {
        hr("");
    }
    public static void hr(String title, String ... params) {
        hr(String.format(title, params));
    }
    public static void hr(String title) {
        System.out.println("======================================================================"+ (Objects.isNull(title) ? "" : title) +"==========================================================================");
    }
}
