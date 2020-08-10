package com.kevin.design.create.builder;

/**
 * @author kevin
 * @date 2020-08-09 19:08:29
 * @desc 纸质包装
 */
public class Wrapper implements Packing {
    @Override
    public String pack() {
        return "Wrapper";
    }
}
