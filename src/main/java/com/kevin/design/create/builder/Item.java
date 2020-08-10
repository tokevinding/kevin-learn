package com.kevin.design.create.builder;

/**
 * @author kevin
 * @date 2020-08-09 19:08:29
 * @desc 汉堡
 */
public interface Item {
    String name();
    float price();
    Packing packing();
}
