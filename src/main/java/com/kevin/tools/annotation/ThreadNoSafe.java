package com.kevin.tools.annotation;

import java.lang.annotation.*;

/**
 * @author kevin
 * @date 2020-08-10 08:21:51
 * @desc 用于标注线程不安全
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThreadNoSafe {
}
