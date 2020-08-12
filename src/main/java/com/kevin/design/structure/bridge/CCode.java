package com.kevin.design.structure.bridge;

import com.kevin.tools.utils.ConsoleOutputUtils;

public class CCode implements Code {
    @Override
    public void code(String programName) {
        ConsoleOutputUtils.println("用 C 编写: " + programName);
    }
}
