package com.kevin.design.create.abstractFactory;

import com.kevin.tools.utils.ConsoleOutputUtils;

/**
 * @author kevin
 * @date 2020-08-09 19:27:19
 * @desc
 */
public class LinuxWriteProgram implements KWriteProgram {
    @Override
    public void write() {
        ConsoleOutputUtils.print("Linux write");
    }
}
