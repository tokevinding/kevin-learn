package com.kevin.spring.components;

import java.util.Objects;

/**
 * @author dinghaifeng
 * @date 2021-04-28 13:32:14
 * @desc
 */
public class CountThreadLocal {
    private static final ThreadLocal<Integer> countThreadLocal = new ThreadLocal();

    public static Integer incrementAndGet() {
        if (Objects.isNull(countThreadLocal.get())) {
            countThreadLocal.set(1);
        } else {
            countThreadLocal.set(countThreadLocal.get() + 1);
        }
        return countThreadLocal.get();
    }
}
