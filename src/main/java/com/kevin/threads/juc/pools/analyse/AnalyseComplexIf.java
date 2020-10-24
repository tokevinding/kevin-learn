package com.kevin.threads.juc.pools.analyse;

/**
 * @author dinghaifeng
 * @date 2020-10-16 17:32:45
 * @desc
 */
public class AnalyseComplexIf extends BaseAnalyse {
    public static void main(String[] args) {

    }

    /**
     * 1. 是否允许添加任务的判断
     * 原始：if (rs >= SHUTDOWN && ! (rs == SHUTDOWN && firstTask == null && ! workQueue.isEmpty()))
     *
     * 转为：if (rs >= SHUTDOWN && (rs != SHUTDOWN || firstTask != null || workQueue.isEmpty()))
     *
     * 拆分：如下三种情况为true(不允许添加线程)
     *     1.rs > SHUTDOWN
     *     2.rs == SHUTDOWN && firstTask != null
     *     3.rs == SHUTDOWN && firstTask == null && workQueue.isEmpty()
     */
    static void addWorkerIfOfFirst() {
    }
}
