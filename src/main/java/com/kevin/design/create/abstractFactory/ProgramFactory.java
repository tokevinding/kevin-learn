package com.kevin.design.create.abstractFactory;

public class ProgramFactory {

    public AbstractProgramFactory getProgramFactory(KSystemEnum systemEnum) {
        if(systemEnum == null){
            return null;
        }
        if(KSystemEnum.WINDOWS.equals(systemEnum)){
            return new WindowsProgramFactory();
        }
        if(KSystemEnum.LINUX.equals(systemEnum)){
            return new LinuxProgramFactory();
        }
        return null;
    }
}
