package com.kevin.design.create.abstractFactory;

import com.kevin.tools.utils.ConsoleOutputUtils;

/**
 * @author kevin
 * @date 2020-08-09 19:27:19
 * @desc
 */
public class WindowsProcessProgram implements KProcessProgram {
    @Override
    public void process() {
        ConsoleOutputUtils.print("Windows process");
    }
}
