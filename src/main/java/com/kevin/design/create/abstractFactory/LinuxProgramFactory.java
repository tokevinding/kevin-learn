package com.kevin.design.create.abstractFactory;


public class LinuxProgramFactory extends AbstractProgramFactory {

    @Override
    public KWriteProgram getWriter() {
        return new LinuxWriteProgram();
    }

    @Override
    public KProcessProgram getProcess() {
        return new LinuxProcessProgram();
    }
}
