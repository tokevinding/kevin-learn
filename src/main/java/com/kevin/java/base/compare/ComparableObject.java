package com.kevin.java.base.compare;

import lombok.Getter;

import java.io.Serializable;

/**
 * @author kevin
 * @date 2021-09-01 18:30:46
 * @desc
 */
public class ComparableObject implements Comparable<ComparableObject>, Serializable {

    @Getter
    private int level;

    public ComparableObject(int level) {
        this.level = level;
    }

    @Override
    public int compareTo(ComparableObject o) {
        return level - o.level;
    }
}
