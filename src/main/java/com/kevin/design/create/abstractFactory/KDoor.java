package com.kevin.design.create.abstractFactory;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KDoor {

    public static void main(String[] args) {
        ProgramFactory programFactory = new ProgramFactory();
        AbstractProgramFactory windowF = programFactory.getProgramFactory(KSystemEnum.WINDOWS);
        windowF.getWriter().write();
        windowF.getProcess().process();

        AbstractProgramFactory linuxF = programFactory.getProgramFactory(KSystemEnum.LINUX);
        linuxF.getWriter().write();
        linuxF.getProcess().process();
    }
}
