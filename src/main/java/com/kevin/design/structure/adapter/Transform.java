package com.kevin.design.structure.adapter;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc 数据传输器
 */
public interface Transform {

    /**
     * 传输数据
     */
    void transformData(TransfromTypeEnum typeEnum);
}
