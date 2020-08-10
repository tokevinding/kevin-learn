package com.kevin.design.create.builder;

/**
 * @author kevin
 * @date 2020-08-09 19:08:29
 * @desc 汉堡
 */
public abstract class ColdDrink implements Item{

    @Override
    public Packing packing() {
        return new Bottle();
    }
}
