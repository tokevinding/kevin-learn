package com.kevin.design.structure.bridge;

import com.kevin.tools.utils.ConsoleOutputUtils;

public class JavaCode implements Code {
    @Override
    public void code(String programName) {
        ConsoleOutputUtils.println("用 JAVA 编写: " + programName);
    }
}
