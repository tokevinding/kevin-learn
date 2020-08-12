package com.kevin.design.structure.bridge;

public abstract class Worker {
    protected Code code;

    protected Worker(Code code){
        this.code = code;
    }

    public abstract void work();
}
