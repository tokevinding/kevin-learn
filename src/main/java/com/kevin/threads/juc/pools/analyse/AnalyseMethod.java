package com.kevin.threads.juc.pools.analyse;

/**
 * @author dinghaifeng
 * @date 2020-10-16 17:32:45
 * @desc
 */
public class AnalyseMethod extends BaseAnalyse {
    public static void main(String[] args) {

    }

    /**
     * execute方法 调用addWork方法 的情况分析
     * 1.addWorker(command, true);
     *  时机：
     *      1）workerCountOf(c) < corePoolSize时
     *  addWorker的处理：（添加失败则结束）
     *      允许创建（核心）线程，处理command
     *
     * 2.addWorker(null, false);
     *  时机：
     *      1）(大于核心线程数 || 1添加失败)
     *              && （isRunning(c) && workQueue.offer(command)）# 是运行状态 并且 添加到队列成功
     *              && （isRunning(recheck) || remove(command)）#复检时是运行状态 或者 非运行状态但移除队列成功（非运行状态 或者 移除失败 走拒绝逻辑）
     *  addWorker的处理：（添加失败不处理）
     *      允许创建（非核心）线程，处理队列中 上面 添加的任务
     *
     * 3.addWorker(command, false);
     *  时机：
     *      1）(大于核心线程数 || 1添加失败)
     *              && （!isRunning(c) || !workQueue.offer(command)）# 非运行状态 并且 添加到队列失败
     *  addWorker的处理：（添加失败走拒绝逻辑）
     *      允许创建（非核心）线程，处理command
     *
     * 总结：
     * 1.活动线程数 小于 核心线程数 创建（核心）线程处理任务
     * 2.活动线程数 大于等于 核心线程数 创建（非核心）线程处理 command及队列 任务
     *      1)添加队列成功 新线程 处理队列任务
     *      1)添加队列失败 新线程 处理 command及队列 任务
     */
    static void addWorkOfExecute() {
    }
}
