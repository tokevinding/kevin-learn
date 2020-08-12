package com.kevin.design.structure.bridge;

/**
 * @author kevin
 * @date 2020-08-09 19:10:46
 * @desc
 */
public class KDoor {
    public static void main(String[] args) {
        TechnologyWorker worker1 = new TechnologyWorker("LAL", new JavaCode());
        TechnologyWorker worker2 = new TechnologyWorker("LAL", new CCode());
        worker1.work();
        worker2.work();
    }
}
