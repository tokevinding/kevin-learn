package com.kevin.design.create.singleton.innerClass;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KDoor {

    public static void main(String[] args) {
        SingleObject.getInstance().showMessage();
        System.out.println("= : " + (SingleObject.getInstance() == SingleObject.getInstance()));
    }
}
