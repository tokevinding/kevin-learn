package com.kevin.design.structure.adapter;

import com.kevin.tools.utils.ConsoleOutputUtils;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc USB外设
 */
public class UsbPeripheral implements Peripheral {

    @Override
    public void transformHdmi() {
    }

    @Override
    public void transformUsb() {
        ConsoleOutputUtils.println("Usb 传输数据");
    }
}
