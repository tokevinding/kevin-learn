package com.kevin.design.structure.adapter;

import com.kevin.tools.utils.ConsoleOutputUtils;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc HDMI外设
 */
public class HdmiPeripheral implements Peripheral {

    @Override
    public void transformHdmi() {
        ConsoleOutputUtils.println("HDMI 传输数据");
    }

    @Override
    public void transformUsb() {
    }
}
