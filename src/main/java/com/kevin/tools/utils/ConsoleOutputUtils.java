package com.kevin.tools.utils;

import com.kevin.common.utils.time.LocalDateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @ClassName: ConsoleOutput
 * @Author: kevin
 * @Time: 2020-03-20 20:06
 * @description: 内容输出
 **/
@Slf4j
public class ConsoleOutputUtils {

    private static int order = 1;

    public static void main(String[] args) {
        LocalDateTime start0 = LocalDateTimeUtils.parseTime("1000-01-01 00:00:00");



        LocalDateTime start = LocalDateTimeUtils.parseTime("2020-08-30 18:27:02");
        LocalDateTime end = LocalDateTimeUtils.parseTime("2020-08-30 18:28:05");




        println(String.valueOf(Duration.between(start0, end).getSeconds()));

        println(String.valueOf(Duration.between(start, end).getSeconds()));
    }

    public static void println(String content) {
        System.out.println(content);
    }
    public static void println(String content, Object ... args) {
        System.out.printf(content+"\n", args);
    }
    public static void hr() {
        hr("");
    }
    public static void hr(String title, String ... params) {
        hr(String.format(title, params));
    }

    private static Map<String, Integer> typeCountMap = new HashMap<>();
    public static void hrLineNumber(String type) {
        hrLineNumber("", type);
    }
    public static void hrLineNumber(String title, String type) {
        Integer itemCount = typeCountMap.get(type);
        if (Objects.nonNull(itemCount)) {
            typeCountMap.put(type, itemCount + 1);
            return;
        }
        typeCountMap.put(type, 1);
        hrNoNumber(typeCountMap.size() + " - " + title + " - "+ type);
    }
    public static void hrNoNumber(String title) {
        System.out.println("======================================================================"+ (Objects.isNull(title) ? "" : title) +"==========================================================================");
    }

    public static void hr(String title) {
        if (StringUtils.isNotEmpty(title)) {
            title = order++ + title;
        }
        System.out.println("======================================================================"+ (Objects.isNull(title) ? "" : title) +"==========================================================================");
    }
    public static void hrl() {
        hrl("");
    }
    public static void hrl(String title, String ... params) {
        hrl(String.format(title, params));
    }
    public static void hrl(String title) {
        if (StringUtils.isNotEmpty(title)) {
            title = order++ + title;
        }
        System.out.println("**********************************************************************"+ (Objects.isNull(title) ? "" : title) +"**************************************************************************");
    }
}
