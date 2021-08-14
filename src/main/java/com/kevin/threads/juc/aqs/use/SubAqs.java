package com.kevin.threads.juc.aqs.use;

/**
 * @author dinghaifeng
 * @date 2021-04-25 18:28:31
 * @desc
 */
public class SubAqs {

    public static void main(String[] args) {
        System.out.println((1<<31) -1);
        int[] ints = new int[(1<<28)];
    }
}
