package com.kevin.design.structure.adapter;

import java.util.Objects;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class TransformAdapter implements Transform {

    private Peripheral peripheral;

    public TransformAdapter(TransfromTypeEnum typeEnum) {
        if (TransfromTypeEnum.USB.equals(typeEnum)) {
            peripheral = new UsbPeripheral();
        } else if (TransfromTypeEnum.HDMI.equals(typeEnum)) {
            peripheral = new HdmiPeripheral();
        }
    }

    @Override
    public void transformData(TransfromTypeEnum typeEnum) {
        if (Objects.isNull(typeEnum)) {
            return;
        }
        if (TransfromTypeEnum.USB.equals(typeEnum)) {
            peripheral.transformUsb();
        }
        if (TransfromTypeEnum.HDMI.equals(typeEnum)) {
            peripheral.transformHdmi();
        }
    }
}
