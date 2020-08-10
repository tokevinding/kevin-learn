package com.kevin.design.create.abstractFactory;

public class WindowsProgramFactory extends AbstractProgramFactory  {
    @Override
    public KWriteProgram getWriter() {
        return new WindowsWriteProgram();
    }
    @Override
    public KProcessProgram getProcess() {
        return new WindowsProcessProgram();
    }
}
