package com.kevin.design.create.abstractFactory;

import com.kevin.design.create.factory.KLog4jLogger;
import com.kevin.design.create.factory.KLoggerEnum;
import com.kevin.design.create.factory.KSlf4jLogger;

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
