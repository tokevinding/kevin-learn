package com.kevin.java.base.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kevin
 * @date 2021-02-20 09:47:22
 * @desc
 */
public class FaceGc {

    public static void main(String[] args) {
        List<byte[]> list = new ArrayList();
        for (int i = 0; i < 10000; i++) {
            list.add(new byte[1024]);
//            System.out.println(list.get(i).length);
//            try {
                System.out.printf("第%s次\n", i+1);
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        System.out.println(list.size());
    }
}
