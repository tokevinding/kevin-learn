package com.kevin.threads.juc.aqs.rw;

/**
 * @author dinghaifeng
 * @date 2020-10-23 14:28:53
 * @desc
 */
public abstract class RwAbstractOwnableSynchronizer implements java.io.Serializable {

    /** Use serial ID even though all fields transient. */
    private static final long serialVersionUID = 3737899427754241961L;

    /**
     * 为子类使用的空构造函数。
     */
    protected RwAbstractOwnableSynchronizer() { }

    /**
     * 独占模式同步的当前所有者。
     */
    private transient Thread exclusiveOwnerThread;

    /**
     * 设置当前拥有独占访问的线程。参数{@code null}表示没有线程拥有访问权。
     * 此方法不强制任何同步或{@code volatile}字段访问。
     * @param thread the owner thread
     */
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    /**
     * 返回最后由{@code setExclusiveOwnerThread}设置的线程，如果未设置，返回{@code null}设置的线程。此方法不会强制执行任何同步或{@code volatile}字段访问。
     * @return the owner thread
     */
    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}