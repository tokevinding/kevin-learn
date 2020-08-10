package com.kevin.design.create.abstractFactory;

/**
 * @author kevin
 * @date 2020-08-09 19:27:19
 * @desc 抽象程序工厂
 */
public abstract class AbstractProgramFactory {
    /**
     * 程序编辑器
     */
    public abstract KWriteProgram getWriter();
    /**
     * 程序执行器
     */
    public abstract KProcessProgram getProcess();
}
