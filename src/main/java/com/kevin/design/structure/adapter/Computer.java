package com.kevin.design.structure.adapter;

import java.util.Objects;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class Computer implements Transform {

    @Override
    public void transformData(TransfromTypeEnum typeEnum) {
        if (Objects.isNull(typeEnum)) {
            return;
        }
        if (TransfromTypeEnum.HARD_DISK.equals(typeEnum)) {
            System.out.println("HARD DISK 数据传输！");
        } else if (TransfromTypeEnum.USB.equals(typeEnum) || TransfromTypeEnum.HDMI.equals(typeEnum)) {
            new TransformAdapter(typeEnum).transformData(typeEnum);
        }
    }
}
