package com.kevin.design.structure.adapter;

import com.kevin.tools.utils.ConsoleOutputUtils;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KDoor {
    public static void main(String[] args) {
        Computer computer = new Computer();
        computer.transformData(TransfromTypeEnum.USB);
        ConsoleOutputUtils.hr();
        computer.transformData(TransfromTypeEnum.HDMI);
        ConsoleOutputUtils.hr();
        computer.transformData(TransfromTypeEnum.HARD_DISK);
    }
}
