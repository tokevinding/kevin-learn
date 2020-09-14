package com.kevin.java.base;

/**
 * @author Kevin
 * @date 2020-09-11 14:28:28
 * @desc
 */
public class BaseHome {

    public static void main(String[] args) {
        Byte a = new Byte((byte) 3);
        Byte b = new Byte((byte) 3);
        System.out.println(a == b);
        Byte c = (byte) 3;
        Byte d = (byte) 3;
        System.out.println(c == d);
        System.out.println(a == d);
    }
}
