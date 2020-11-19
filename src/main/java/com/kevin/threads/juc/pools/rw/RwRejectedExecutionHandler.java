package com.kevin.threads.juc.pools.rw;

import java.util.concurrent.RejectedExecutionException;

/**
 * @author kevin
 * @date 2020-10-22 17:06:44
 * @desc
 */
public interface RwRejectedExecutionHandler {

    /**
     * Method that may be invoked by a {@link RwThreadPoolExecutor} when
     * {@link RwThreadPoolExecutor#execute execute} cannot accept a
     * task.  This may occur when no more threads or queue slots are
     * available because their bounds would be exceeded, or upon
     * shutdown of the Executor.
     *
     * <p>In the absence of other alternatives, the method may throw
     * an unchecked {@link RejectedExecutionException}, which will be
     * propagated to the caller of {@code execute}.
     *
     * @param r the runnable task requested to be executed
     * @param executor the executor attempting to execute this task
     * @throws RejectedExecutionException if there is no remedy
     */
    void rejectedExecution(Runnable r, RwThreadPoolExecutor executor);
}
