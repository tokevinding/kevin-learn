package com.kevin.design.create.builder;

/**
 * @author kevin
 * @date 2020-08-09 19:08:29
 * @desc 百事可乐
 */
public class Pepsi extends ColdDrink {

    @Override
    public float price() {
        return 35.0f;
    }

    @Override
    public String name() {
        return "Pepsi";
    }

}
