package com.kevin.design.structure.bridge;

public class TechnologyWorker extends Worker {

    private String programName;

    public TechnologyWorker(String programName, Code code){
        super(code);
        this.programName = programName;
    }

    @Override
    public void work() {
        code.code(programName);
    }
}
