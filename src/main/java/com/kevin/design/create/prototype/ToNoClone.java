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
public class ToNoClone implements Serializable {

    private String name;
}
