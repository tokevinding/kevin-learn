package com.kevin.java.base.compare;

import java.util.Comparator;

/**
 * @author kevin
 * @date 2021-09-01 18:30:46
 * @desc
 */
public class ComparatorObject implements Comparator<ComparableObject> {

    @Override
    public int compare(ComparableObject o1, ComparableObject o2) {
        return o2.getLevel() - o1.getLevel();
    }
}
