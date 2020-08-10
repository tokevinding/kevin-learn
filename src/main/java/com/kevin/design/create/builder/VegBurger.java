package com.kevin.design.create.builder;

/**
 * @author kevin
 * @date 2020-08-09 19:08:29
 * @desc 素食汉堡
 */
public class VegBurger extends Burger {

    @Override
    public float price() {
        return 25.0f;
    }

    @Override
    public String name() {
        return "Veg Burger";
    }
}
