package com.kevin.design.create.singleton.enums;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KDoor {

    public static void main(String[] args) {
        SingleObject.INSTANCE.showMessage();
        System.out.println("= : " + (SingleObject.INSTANCE == SingleObject.INSTANCE));
    }
}
