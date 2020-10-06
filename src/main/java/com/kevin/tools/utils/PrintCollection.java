package com.kevin.tools.utils;

import com.kevin.tools.SystemContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * program: kevinutils
 * author: kevin
 * create time: 2019-08-21 11:30
 * description: 集合处理工具
 **/
@Slf4j
public class PrintCollection {

    /* *******************************************打印多集合********************************************/
    /**
     * 默认title打印多集合
     */
    public static void defaultPrint(Collection<?> ... collections) {
        Arrays.stream(collections).forEach(collection -> printCollection(collection, "打印集合", ","));
    }

    /**
     * 默认title打印多集合
     */
    public static void defaultPrintln(Collection<?> ... collections) {
        defaultPrintln("", collections);
    }

    /**
     * 默认title打印多集合
     */
    public static void defaultPrintln(String title, Collection<?> ... collections) {
        Arrays.stream(collections).forEach(collection -> {
            Print.printfln("======================%s - size: %s======================\n", title, collection.size());
            if (CollectionUtils.isEmpty(collection)) {
                return;
            }
            collection.forEach(Print::println);
        });
    }


    public static void defaultPrint(String title, Collection<?> ... collections) {
        for (Collection<?> collection : collections) {
            printCollection(collection, title, ",");
        }
    }
    /**
     * 默认title打印多集合
     * 如：[["1", "1"],["1", "1"]]
     *
     * 打印内容为：
     * '1', '1'
     *
     * '1', '1'
     */
    public static void defaultDecoratePrint(String title, Collection<?> ... collections) {
        for (Collection<?> collection : collections) {
            printCollection(PrintCollection.decorateCollection(collection, "'", "'"), title, ",");
        }
    }

    /**
     * 默认title打印多集合
     */
    public static void defaultDecoratePrint(Collection<?> ... collections) {
        for (Collection<?> collection : collections) {
            printCollection(PrintCollection.decorateCollection(collection, "'", "'"), "集合内容输出", ",");
        }
    }

    /* *******************************************打印单集合********************************************/
    /**
     * 分批打印（默认打印）
     */
    public static void defaultBatchPrint(Collection<?> collection) {
        defaultBatchConsumer(collection, PrintCollection::defaultPrint);
    }

    /**
     * 分批打印（修饰打印）
     */
    public static void defaultBatchDecoratePrint(Collection<?> collection) {
        defaultBatchConsumer(collection, PrintCollection::defaultDecoratePrint);
    }

    /**
     * 分批打印
     */
    public static void defaultBatchConsumer(Collection<?> collection, BiConsumer<String, Collection> consumer) {
        int iteratorCount = BatchCountUtils.count(collection.size());
        Print.println("==============处理总量：" + collection.size() + "==============");
        for (int i = 0; i < iteratorCount; i++) {
            Print.println("==============第" +i+ "行==============");
            List<?> collect = collection.stream()
                    .skip(i * SystemContent.DEFAULT_SINGLE_BATCH_DEAL)
                    .limit(SystemContent.DEFAULT_SINGLE_BATCH_DEAL).collect(Collectors.toList());
            consumer.accept("第" +i+ "批", collect);
        }
    }

    /**
     * 集合内容打印
     */
    public static void printCollection(Collection<?> collection, String title, String seperate) {
        Print.printfln("======================%s - size: %s======================", title, collection.size());
        if (CollectionUtils.isEmpty(collection)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        collection.forEach(item -> sb.append(item.toString()).append(seperate));
        String result = sb.toString();
        result = result.length() < 1 ? result : result.substring(0, result.length() - 1);
        Print.printlns(result, 2);
    }

    /* *******************************************集合装饰********************************************/
    /**
     * 集合添加前后坠
     */
    public static Collection<String> decorateCollection(Collection<?> collection, String pre, String tail) {
        return collection.stream().map(item -> pre + item.toString() + tail).collect(Collectors.toList());
    }
}
