package com.kevin.design.create.prototype;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
@ToString
@AllArgsConstructor
public class ToClone implements Cloneable, Serializable {

    private String name;

    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
