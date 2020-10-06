package com.kevin.java.base.loader.notInPath;

/**
 * @author dinghaifeng
 * @date 2020-09-30 11:38:57
 * @desc
 */
public class NotInClassPathClass {
    private String name = "Not in path class";

    public String getName() {
        System.out.println("do getName method!");
        return name;
    }
}
