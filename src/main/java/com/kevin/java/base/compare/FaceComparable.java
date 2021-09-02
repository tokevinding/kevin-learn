package com.kevin.java.base.compare;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author dinghaifeng
 * @date 2021-09-01 18:30:32
 * @desc
 */
public class FaceComparable {
    public static void main(String[] args) {
        List<ComparableObject> list = Lists.newArrayList();
        List<ComparableObject> list1 = Lists.newArrayList();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            ComparableObject o = new ComparableObject(random.nextInt(100));
            list.add(o);
            list1.add(o);
        }
        list.forEach(item -> System.out.print(item.getLevel() + " "));
        System.out.println();
        Collections.sort(list);
        list.forEach(item -> System.out.print(item.getLevel() + " "));
        System.out.println();

        Collections.sort(list1, new ComparatorObject());
        list1.forEach(item -> System.out.print(item.getLevel() + " "));
        System.out.println();


    }
}
