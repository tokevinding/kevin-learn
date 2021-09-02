package com.kevin.java.base.compare;

import lombok.Getter;

import java.io.Serializable;

/**
 * @author dinghaifeng
 * @date 2021-09-01 18:30:46
 * @desc
 */
public class ComparetorObject implements Comparable<ComparetorObject>, Serializable {

    @Getter
    private int level;

    public ComparetorObject(int level) {
        this.level = level;
    }

    @Override
    public int compareTo(ComparetorObject o) {
        return level - o.level;
    }
}
