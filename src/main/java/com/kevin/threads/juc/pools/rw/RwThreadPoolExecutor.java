package com.kevin.threads.juc.pools.rw;

import com.kevin.threads.juc.aqs.rw.RwAbstractQueuedSynchronizer;
import lombok.Getter;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author kevin
 * @date 2020-10-22 17:01:55
 * @desc
 */
public class RwThreadPoolExecutor extends AbstractExecutorService {

    protected static final int COUNT_BITS = Integer.SIZE - 3;
    /**
     * 最大容量: 000 11111111111111111111111111111
     */
    protected static final int CAPACITY = (1 << COUNT_BITS) - 1;

    /* ***************************状态常量（只有状态位）*****************************/
    /**
     * 运行中状态
     * 111 00000000000000000000000000000
     */
    protected static final int RUNNING = -1 << COUNT_BITS;

    // runState is stored in the high-order bits

    /**
     * 000 00000000000000000000000000000
     */
    protected static final int SHUTDOWN = 0 << COUNT_BITS;
    /**
     * 001 00000000000000000000000000000
     */
    protected static final int STOP = 1 << COUNT_BITS;
    /**
     * 010 00000000000000000000000000000
     */
    protected static final int TIDYING = 2 << COUNT_BITS;
    /**
     * 011 00000000000000000000000000000
     */
    protected static final int TERMINATED = 3 << COUNT_BITS;

    public AtomicInteger getCtl() {
        return ctl;
    }

    /**
     * 默认的拒绝执行处理程序，默认抛异常
     */
    private static final RwRejectedExecutionHandler defaultHandler = new RwThreadPoolExecutor.AbortPolicy();

    // Packing and unpacking ctl
    /**
     * 请求shutdown和shutdownNow需要权限。我们还需要(参见checkShutdownAccess)调用者拥有实际中断工作集中
     * (由依赖于ThreadGroup的Thread.interrupt管理)线程的权限。
     * checkAccess，这又依赖于SecurityManager.checkAccess)。只有当这些检查通过时才尝试关闭。所有对Thread.interrupt的实际调用
     * (参见interruptIdleWorkers和interruptWorkers)都会忽略securityexception，这意味着尝试的中断会以静默方式失败。在关闭的情况下
     * ，它们不应该失败，除非SecurityManager有不一致的策略，有时允许访问线程，有时不。在这种情况下，无法实际中断线程可能会禁用或延迟完全终止。
     * interruptIdleWorkers的其他用途是咨询的，而没有实际中断只会延迟对配置更改的响应，因此不处理例外情况。
     */
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");
    private static final boolean ONLY_ONE = true;
    protected final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    /**
     * 用于保存任务并将其传递给worker 线程的队列。我们不要求workQueue.poll()返回
     * null就一定意味着workQueue.isEmpty()，所以只依赖于来查看队列是否为空
     * (例如，在决定是否从 SHUTDOWN过渡到整理时，我们必须这样做)。
     * 这适用于特殊用途的队列，比如允许poll()返回null的DelayQueues，即使在延迟到期后它可能返回非null。
     */
    private final BlockingQueue<Runnable> workQueue;
    /**
     * 存取work加锁。虽然我们可以使用某种类型的并发集，但使用锁通常更可取。
     * 其中一个原因是，这种方法序列化了interruptIdleWorkers，从而避免了不必要的中断，特别是在关机期间。
     * 否则，正在退出的线程将并发地中断那些尚未被中断的。它还简化了一些与largestPoolSize等相关的统计记账。
     * 我们也持有mainLock的关机和关机现在，为了确保work设置是稳定的，同时分别检查允许中断和实际中断。
     */
    private final ReentrantLock mainLock = new ReentrantLock();
    /**
     * 包含池中所有工作线程的集合。仅在持有主锁时可访问。
     */
    @Getter
    private final HashSet<RwThreadPoolExecutor.Worker> workers = new HashSet<>();
    /**
     * 等待条件，以支持等待终止
     */
    private final Condition termination = mainLock.newCondition();
    /* 执行终结器时使用的上下文，或null */
    private final AccessControlContext acc;
    /**
     * 跟踪获得的最大池大小。仅在主锁下访问。
     */
    private int largestPoolSize;
    /**
     * 已完成任务的计数器。仅在工作线程终止时更新。仅在主锁下访问。
     */
    private long completedTaskCount;
    /**
     * 工厂为新线程。所有线程都是使用这个工厂创建的(通过方法addWorker)。所有调用方都必须为addWorker失败做好准备
     * ，这可能反映了系统或用户的策略限制了线程的数量。即使没有将其视为错误，但创建线程失败可能会导致拒绝新任务或现有任务仍停留在队列中。
     * 甚至在遇到OutOfMemoryError之类的错误时，我们还会进一步保留池不变量，这些错误可能会在尝试创建线程时抛出。
     * 由于需要在线程中分配本机堆栈，此类错误相当常见。启动时，用户将希望执行清理池关闭以进行清理。
     * 可能会有足够的内存可用来完成清理代码，而不会遇到另一个OutOfMemoryError错误。
     */
    private volatile ThreadFactory threadFactory;
    /**
     * 处理程序在执行中饱和或关闭时调用
     */
    private volatile RwRejectedExecutionHandler handler;
    /**
     * 空闲线程等待工作的超时(以纳秒为单位)。当存在超过corePoolSize或者allowCoreThreadTimeOut时，线程会使用此超时。否则，他们永远等待新工作。
     */
    private volatile long keepAliveTime;
    /**
     * 如果为false(默认)，则核心线程即使在空闲时也保持活动。如果为真，核心线程使用keepAliveTime来超时等待工作。
     */
    private volatile boolean allowCoreThreadTimeOut;
    /**
     * 核心池大小是保持活动的工作线程的最小数量(并且不允许超时等)，除非设置了allowCoreThreadTimeOut，在这种情况下，最小值为零。
     */
    private volatile int corePoolSize;

    /*
     * 所有用户控制参数都声明为volatile，这样正在进行的操作就基于最新的值，但不需要锁定，因为内部不变量依赖于它们相对于其他操作同步变化。
     */
    /**
     * 最大池大小。注意，实际的最大值在内部受到容量的限制。
     */
    private volatile int maximumPoolSize;

    /**
     * 使用给定的初始参数和默认的线程工厂和被拒绝的执行处理程序创建一个新的{@code RwThreadPoolExecutor}。
     * 使用一个 @link executor 工厂方法可能比使用这个通用的构造函数更方便。
     *
     * @param corePoolSize    池中保留的线程数，即使它们是空闲的，除非设置了{@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 池中允许的最大线程数
     * @param keepAliveTime   当线程数量大于核心时，这是多余空闲线程在终止之前等待新任务的最大时间。
     * @param unit            参数{@code keepAliveTime}的时间单位
     * @param workQueue       在执行任务之前用于保存任务的队列。此队列将只保存由{@code execute}方法提交的{@code Runnable}任务。
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue} is null
     */
    public RwThreadPoolExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * 使用给定的初始参数和默认的拒绝执行处理程序创建一个新的{@code RwThreadPoolExecutor}。
     *
     * @param corePoolSize    池中保留的线程数，即使它们是空闲的，除非设置了{@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 池中允许的最大线程数
     * @param keepAliveTime   当线程数量大于核心时，这是多余空闲线程在终止之前等待新任务的最大时间。
     * @param unit            参数{@code keepAliveTime}的时间单位
     * @param workQueue       在执行任务之前用于保存任务的队列。此队列将只保存由{@code execute}方法提交的{@code Runnable}任务。
     * @param threadFactory   执行程序创建新线程时要使用的工厂
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code threadFactory} is null
     */
    public RwThreadPoolExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue,
                                ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, defaultHandler);
    }

    /**
     * 使用给定的初始参数和默认的线程工厂创建一个新的{@code RwThreadPoolExecutor}。
     *
     * @param corePoolSize    池中保留的线程数，即使它们是空闲的，除非设置了{@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 池中允许的最大线程数
     * @param keepAliveTime   当线程数量大于核心时，这是多余空闲线程在终止之前等待新任务的最大时间。
     * @param unit            参数{@code keepAliveTime}的时间单位
     * @param workQueue       在执行任务之前用于保存任务的队列。此队列将只保存由{@code execute}方法提交的{@code Runnable}任务。
     * @param handler         当执行因达到线程边界和队列容量而阻塞时使用的处理程序
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code handler} is null
     */
    public RwThreadPoolExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue,
                                RwRejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }

    /**
     * 使用给定的初始参数创建一个新的{@code RwThreadPoolExecutor}。
     *
     * @param corePoolSize    池中保留的线程数，即使它们是空闲的，除非设置了{@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 池中允许的最大线程数
     * @param keepAliveTime   当线程数量大于核心时，这是多余空闲线程在终止之前等待新任务的最大时间。
     * @param unit            参数{@code keepAliveTime}的时间单位
     * @param workQueue       在执行任务之前用于保存任务的队列。此队列将只保存由{@code execute}方法提交的{@code Runnable}任务。
     * @param threadFactory   执行程序创建新线程时要使用的工厂
     * @param handler         当执行因达到线程边界和队列容量而阻塞时使用的处理程序
     * @throws IllegalArgumentException 如果下列条件之一成立:<br>
                                  {@code corePoolSize < 0}<br>
                                  {@code keepAliveTime < 0}<br>
                                  {@code maximumPoolSize <= 0}<br>
                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code threadFactory} or {@code handler} is null
     */
    public RwThreadPoolExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue,
                                ThreadFactory threadFactory,
                                RwRejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0) {
            throw new IllegalArgumentException();
        }
        if (workQueue == null || threadFactory == null || handler == null) {
            throw new NullPointerException();
        }
        this.acc = System.getSecurityManager() == null ?
                null :
                AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * 获取当前运行状态
     * ~CAPACITY ：111 00000000000000000000000000000
     * 取与，数量位全为0，状态位依据 c 确定
     */
    protected static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    /**
     * CAPACITY ：000 11111111111111111111111111111
     * 取与，状态位全为0，数量位依据 c 确定
     */
    protected static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    /**
     * 合并 状态位 和 数量位
     *
     * @param rs 状态位 xxx 00000000000000000000000000000
     * @param wc 数量位 000 xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     * @return 合并后的结果
     */
    protected static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    /**
     * c小于某个状态
     */
    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    /**
     * c大于等于某个状态
     */
    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    /*
     * 用于设置控件状态的方法
     */

    /**
     * RUNNING状态下的 c 都是小于0的，SHUTDOWN = 0
     */
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * 尝试case workerCount+1（ctl+1）(允许失败)
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /*
     * 控制工作线程中断的方法。
     */

    /**
     * 尝试case workerCount-1（ctl-1）(允许失败)
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * cas workerCount-1（ctl-1）(直到成功)
     */
    private void decrementWorkerCount() {
        do {
        } while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    /**
     * 将运行状态转换到给定的目标，或者如果至少已经是给定的目标，则不影响它。
     *
     * @param targetState 所需的状态，关闭或停止(但不整理或终止——为此使用tryTerminate)
     */
    private void advanceRunState(int targetState) {
        for (; ; ) {
            int c = ctl.get();
            // 1.当前池状态已大于或等于目标状态则不需要改变 2.改变状态成功
            if (runStateAtLeast(c, targetState) ||
                    ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) {
                break;
            }
            //当前池状态小于目标状态 && 改变状态失败 执行循环
        }
    }

    /**
     * 转换到终止状态，如果(关闭和池和队列为空)或(停止和池为空)。如果workerCount非零，但符合终止条件，则中断空闲的worker以确保关闭信号传播。
     * 必须在任何可能使终止成为可能的操作(减少工作人员数量或在关闭期间从队列中删除任务)之后调用此方法。
     * 该方法是非私有的，允许从ScheduledThreadPoolExecutor访问。
     */
    final void tryTerminate() {
        for (; ; ) {
            int c = ctl.get();
//          原：  if (isRunning(c) || runStateAtLeast(c, TIDYING) || (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty())) {
            /*不允许执行终止逻辑的几种情况：*/
            //1.RUNNING状态
            if (isRunning(c)) {
                return;
            }
            //2.STOP及之后的状态
            if (runStateAtLeast(c, TIDYING)) {
                return;
            }
            //3.SHUTDOWN状态 队列非空 （需要处理队列任务）
            if (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty()) {
                return;
            }

            //活动线程数不等于0，中断可能正在等待任务的线程（资格终止）
            if (workerCountOf(c) != 0) {
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            //走到这里，说明活动线程数等于0！！
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                //设置池状态1 为： TIDYING
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        //已执行terminated，设置池状态2 为：TERMINATED
                        ctl.set(ctlOf(TERMINATED, 0));
                        //唤醒终止
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // 设置池状态1失败，执行CAS重试
        }
    }

    /**
     * 如果存在安全管理器，请确保调用者通常拥有关闭线程的权限(参见shutdownPerm)。
     * 如果它通过，另外确保调用者被允许中断每个工作线程。
     * 如果SecurityManager专门处理某些线程，即使第一次检查通过，这也可能不是真的。
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (RwThreadPoolExecutor.Worker w : workers) {
                    security.checkAccess(w.thread);
                }
            } finally {
                mainLock.unlock();
            }
        }
    }

    /*
     * Misc实用程序，其中大部分也被导出到ScheduledThreadPoolExecutor
     */

    /**
     * 中断所有线程，即使是活动的。忽略securityexception(在这种情况下，一些线程可能保持不间断)。
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (RwThreadPoolExecutor.Worker w : workers) {
                w.interruptIfStarted();
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 中断可能正在等待任务的线程(表示没有被锁定)，以便它们可以检查终止或配置更改。忽略securityexception(在这种情况下，一些线程可能保持不间断)。
     *
     * @param onlyOne 如果为真，最多中断一个worker。只有在启用了终止时才从tryTerminate调用，但仍然有其他workers。
                      在这种情况下，在所有线程当前都在等待的情况下，最多中断一个等待的worker以传播shutown信号。
                      中断任意线程可以确保在关闭开始后新来的worker最终也会退出。为了保证最终的终止，始终只中断一个空闲的worker就足够了
                      ，但是shutdown()会中断所有空闲的worker，这样冗余的worker就会立即退出，而不会等待一个掉队的任务完成。
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (RwThreadPoolExecutor.Worker w : workers) {
                Thread t = w.thread;
                //线程未标记中断 && 允许中断
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        //标记中断
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                //如果只中断一个，直接跳出（无论检查标记成功与否）
                if (onlyOne) {
                    break;
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 中断工作者的常见形式，以避免必须记住布尔参数的意思。
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    /**
     * 为给定命令调用被拒绝的执行处理程序。
     * 由ScheduledThreadPoolExecutor使用的包保护。
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /*
     * 创建、运行和清理工人的方法
     */
    /**
     * 在调用shutdown时执行运行状态转换之后执行进一步清理。
     * 这里是一个空操作，但由ScheduledThreadPoolExecutor用于取消延迟的任务。
     */
    void onShutdown() {
    }

    /**
     * ScheduledThreadPoolExecutor在关闭期间启用运行任务所需的状态检查。
     *
     * @param shutdownOK 如果关闭，则返回true
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * 将任务队列排空到一个新列表中，通常使用drainTo。
     * 但是，如果队列是DelayQueue或轮询或析链可能无法删除某些元素的任何其他类型的队列，则逐个删除它们。
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        return taskList;
    }

    /**
     * 检查是否可以根据当前池状态和给定的边界(核心或最大值)添加新的worker。
     * 如果是，则相应地调整worker计数，并且，如果可能，将创建并启动一个新的worker，并将firstTask作为它的第一个任务运行。
     * 如果池已停止或符合关闭条件，则此方法返回false。如果线程工厂在请求时未能创建线程，它还返回false。
     * 如果线程创建失败，或者是由于线程工厂返回null，或者是由于异常(通常是thread.start()中的OutOfMemoryError错误)，我们将干净地回滚。
     *
     * @param firstTask 新线程首先运行的任务(如果没有，则为空)。当corePoolSize线程少于一个时(在方法execute()中)，
     *                  或者当队列已满时(在这种情况下，我们必须绕过队列)，工人会用一个初始的第一个任务创建(在方法execute()中)，以绕过队列。
     *                  最初，空闲线程通常是通过prestartCoreThread创建的，或者用来替换其他垂死的工作线程。
     * @param core      如果为真，则使用corePoolSize作为绑定，否则使用maximumPoolSize。(这里使用布尔指示器而不是值来确保在检查其他池状态后读取新值)。
     * @return true 如果成功
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        //外循环
        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // 只在必要时检查队列是否为空。
//          原：  if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty())) {

            /*不允许加任务/线程的几种情况：*/
            //1.STOP, TIDYING, TERMINATED状态
            if (rs > SHUTDOWN) {
                return false;
            }

            //2.SHUTDOWN状态 && 传入任务非空( SHUTDOWN状态 不允许添加新任务)
            if (rs == SHUTDOWN && firstTask != null) {
                return false;
            }

            //3.SHUTDOWN状态 && 传入任务为空 && 队列为空（此时已经不需要添加线程了）
            if (rs == SHUTDOWN && workQueue.isEmpty()) {
                return false;
            }

            //内循环
            for (; ; ) {
                //获取活动线程数
                int wc = workerCountOf(c);
//              原：  if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize)) {
                /*不允许添加线程的几种情况：*/
                //1.超出最大容量
                if (wc >= CAPACITY) {
                    return false;
                }
                //2.只创建核心线程标识时 - 超出核心线程数
                if (core && wc >= corePoolSize) {
                    return false;
                }
                //2.允许创建至最大线程时 - 超出最大线程数
                if (!core && wc >= maximumPoolSize) {
                    return false;
                }

                //ctl自增1
                if (compareAndIncrementWorkerCount(c)) {
                    //自增成功-跳出 外循环
                    break retry;
                }
                // 重读ctl
                c = ctl.get();
                //自增失败&&状态改变-重新执行 外循环
                if (runStateOf(c) != rs) {
                    continue retry;
                }
                // 因workerCount更改导致CAS失败;重试内循环
            }
        }

        //走到这里-允许添加线程

        //标识 新线程 已经启动
        boolean workerStarted = false;
        //标识 新worker 已经添加到 workers列表中
        boolean workerAdded = false;
        RwThreadPoolExecutor.Worker w = null;
        try {
            //创建一个新 worker（会创建一个 Worker 作 Runnable 的线程）
            w = new RwThreadPoolExecutor.Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // 保持锁定状态时再次检查。退出线程工厂失败或如果在获得锁之前关闭。
                    int rs = runStateOf(ctl.get());

                    /*
                     * 两种情况：
                     * 1.rs < SHUTDOWN (RUNNING状态)：正常的创建线程的池状态
                     * 2.rs == SHUTDOWN && firstTask == null：前面移除队列失败并且无线程处理任务时，创建线程用于处理队列任务
                     */
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        //预先检查t是可启动的
                        if (t.isAlive()) {
                            throw new IllegalThreadStateException();
                        }
                        workers.add(w);
                        int s = workers.size();
                        //什么情况下会为false？
                        if (s > largestPoolSize) {
                            largestPoolSize = s;
                        }
                        //标识已添加
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    //启动线程
                    t.start();
                    //标识已启动
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted) {
                //启动失败的处理
                addWorkerFailed(w);
            }
        }
        return workerStarted;
    }

    /**
     * 回滚工作线程的创建。
     * 将worker从workers中移除，如果目前workers数量减少，重新检查终止，以防这种情况的存在
     */
    private void addWorkerFailed(RwThreadPoolExecutor.Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null) {
                workers.remove(w);
            }
            //能走到这里，说明之前WorkerCount自增成功了，所以这里需要自减
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    // 公共构造函数和方法

    /**
     * 为垂死的worker做清洁和记账工作。仅从工作线程调用。除非突然完成设置，假设workerCount已经调整到考虑退出。
     * 此方法从工作线程集中删除线程，如果由于用户任务异常退出工作线程，或者运行的工作线程少于corePoolSize，或者队列非空但没有工作线程，则可能终止线程池或替换该工作线程。
     *
     * @param w                 the worker
     * @param completedAbruptly 如果worker死于用户异常
     */
    private void processWorkerExit(RwThreadPoolExecutor.Worker w, boolean completedAbruptly) {
        //如果突然结束（用户业务异常导致中断），则workerCount没有被调整
        if (completedAbruptly) {
            //活动线程数自减
            decrementWorkerCount();
        }

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //统计所有线程完成任务的数量
            completedTaskCount += w.completedTasks;
            //移除worker
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        //可能终止，调用
        tryTerminate();

        int c = ctl.get();
        //池状态小于STOP：RUNNING, SHUTDOWN
        if (runStateLessThan(c, STOP)) {
            //非用户业务异常终止
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                //条件为：(允许核心线程数超时 或 核心线程数为0) 并 队列不为空
                if (min == 0 && !workQueue.isEmpty()) {
                    min = 1;
                }
                //确保至少有一个活动线程
                if (workerCountOf(c) >= min) {
//                    正常的情况 - 不需要增加：
//                    1.允许核心线程过期 并且 队列非空 并且 活动线程 >= 1
//                    2.不允许核心线程过期 并且 活动线程 >= 核心线程数
                    return;
                }
            }
            //添加一个worker，替换本worker（开一个新线程）
            addWorker(null, false);
        }
    }

    /**
     * 执行阻塞或定时等待任务，取决于当前的配置设置，或返回null，
     * 如果这个work退出的原因:
     * 1. 有不止maximumPoolSize工作程序(由于调用setMaximumPoolSize)。
     * 2. 池子停止了。
     * 3. 池被关闭并且队列为空。
     * 4. workQueue.poll超时等待任务 在定时等待之前和之后，worker都会被终止
     *      (即{@code allowCoreThreadTimeOut || workerCount > corePoolSize})
     *      ，如果队列不是空的，这个worker就不是池中的最后一个线程。
     *
     * @return 任务，在这种情况下，如果工作者必须退出，则为null
     * workerCount是递减
     */
    private Runnable getTask() {
        //上次获取任务是否超时
        boolean timedOut = false;

        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // 只在必要时检查队列是否为空。
            //两种情况：
            //1.SHUTDOWN状态队列为空
            //2.STOP状态
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            // 是否允许超时 终止线程
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            if ((wc > maximumPoolSize || (timed && timedOut))
                    && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c)) {
                    return null;
                }
                continue;
            }

            try {
                Runnable r = timed ?
                        workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                        workQueue.take();
                if (r != null) {
                    return r;
                }
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }

    /**
     * 主工作程序运行循环。从队列中反复获取任务并执行它们，同时处理一些问题:
     * 1. 我们可以从一个初始任务开始，在这种情况下，我们不需要获得第一个任务。否则，只要池在运行，我们就会从getTask获得任务。
     * 如果返回null，则工作程序将由于池状态或配置参数的更改而退出。其他退出是由于抛出外部代码中的异常导致的，
     * 在这种情况下complete突然保持，这通常会导致processWorkerExit替换此线程。
     *
     * 2. 在运行任何任务之前，会获得锁，以防止任务执行时其他池中断，然后我们确保除非池停止，否则这个线程不会设置它的中断。
     *
     * 3.在每个任务运行之前，都会调用beforeExecute，这可能会抛出一个异常，在这种情况下，我们会导致线程在不处理任务的情况下死亡(用completedsuddenly true中断循环)。
     *
     * 4. 假设beforeExecute正常完成，我们运行任务，收集它抛出的任何异常发送到afterExecute。我们分别处理RuntimeException、Error(规范保证会捕获这两者)和任意可抛掷事件。
     * 因为我们不能在Runnable.run中重新抛出可抛弃物，所以我们在抛出时将它们包装在错误中(到线程的UncaughtExceptionHandler中)。保守地说，抛出的任何异常都会导致线程死亡。
     *
     * 5. 在task.run完成后，我们调用afterExecute，这也会抛出一个异常，这也会导致线程死亡。根据JLS第14.20秒，即使task.run抛出，这个异常也会生效。
     * 异常机制的最终效果是，在执行后，线程的UncaughtExceptionHandler提供了我们所能提供的关于用户代码遇到的任何问题的同样准确的信息。
     *
     * @param w the worker
     */
    final void runWorker(RwThreadPoolExecutor.Worker w) {
        Thread wt = Thread.currentThread();
        // firstTask可能存在为空的情况
        Runnable task = w.firstTask;
        w.firstTask = null;
        // 允许中断
        w.unlock();
        boolean completedAbruptly = true;
        try {
            //获取任务：1.首任务非空，执行首任务 2.首任务为空，从队列中获取
            while (task != null || (task = getTask()) != null) {
                //不允许中断
                w.lock();
                // 如果池停止，确保线程被中断;如果没有，确保线程没有被中断。这需要在第二种情况下重新检查，以处理在清除中断时立即关闭比赛

//              原：  if ((runStateAtLeast(ctl.get(), STOP) || (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP))) && !wt.isInterrupted()) {

                /*需要标记中断的几种情况：*/
                //1.池状态大于RUNNING，并且未被标记中断
                if (runStateAtLeast(ctl.get(), STOP) && !wt.isInterrupted()) {
                    wt.interrupt();
                }
                //2.线程被中断(清除了中断标记)，池状态大于RUNNING 并且未被标记中断（已标记线程中断，线程池还未标记STOP时，争取时间片）
                if ((Thread.interrupted() && runStateAtLeast(ctl.get(), STOP)) && !wt.isInterrupted()) {
                    wt.interrupt();
                }
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        //任务都实现了Runnable接口，直接执行任务的run方法
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x;
                        throw x;
                    } catch (Error x) {
                        thrown = x;
                        throw x;
                    } catch (Throwable x) {
                        thrown = x;
                        throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    //task置空，协助垃圾回收，同时让循环作出正确判断
                    task = null;
                    //已完成任务+1（任务抛异常也会执行）
                    w.completedTasks++;
                    //标记可中断
                    w.unlock();
                }
            }
            //任务正常完成-无用户业务异常
            completedAbruptly = false;
        } finally {
            //Worker退出逻辑
            processWorkerExit(w, completedAbruptly);
            System.out.println("线程" + Thread.currentThread().getName()+ "挂掉");
        }
    }

    /**
     * 在将来的某个时候执行给定的任务。任务可以在新线程中执行，也可以在现有的池线程中执行。
     * 如果任务不能被提交执行，要么因为这个执行器已经关闭，要么因为它的容量已经达到，任务被当前的{@code RwRejectedExecutionHandler}处理。
     *
     * @param command 要执行的任务
     * @throws RejectedExecutionException 如果任务不能被接受执行，请使用{@code RwRejectedExecutionHandler}
     * @throws NullPointerException       if {@code command} is null
     */
    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        /*
         * Proceed in 3 steps:
         *
         * 1. 如果运行的线程少于corePoolSize，则尝试使用给定命令作为其第一个任务启动一个新线程。
         * 对addWorker的调用会自动检查runState和workerCount，从而通过返回false防止在不应该添加线程的情况下添加线程的错误警报。
         *
         * 2. 如果任务可以成功排队，那么我们仍然需要再次检查是否应该添加一个线程(因为现有的线程在上次检查后死亡)，或者池在进入此方法后关闭。
         * 因此，我们会重新检查状态，如果停止队列，必要时回滚队列;如果没有线程，则启动一个新线程。
         *
         * 3. 如果我们不能将任务放入队列，那么我们尝试添加一个新线程。如果它失败了，我们知道我们被关闭或饱和，因此拒绝这个任务。
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true)) {
                return;
            }
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            //运行状态 && 加入队列成功
            int recheck = ctl.get();
            if (!isRunning(recheck) && remove(command)) {
                //复检非运行状态 && 移除队列成功
                reject(command);
            } else if (workerCountOf(recheck) == 0) {
                //（复检运行状态 or 移除队列失败）并且活动线程数=0时，创建一个用于处理队列任务（比如 核心线程数为0时，上面已经把任务加入到队列了）
                addWorker(null, false);
            }
        } else if (!addWorker(command, false)) {
            //非运行状态 OR 加入队列失败 拒绝
            reject(command);
        }
    }

    /**
     * 启动有序关闭，在该关闭中执行先前提交的任务，但不接受新任务。如果已经关闭，调用没有额外的影响。
     * 此方法不等待先前提交的任务完成执行。使用{@link #awaitTermination awaitTermination}来完成此操作。
     *
     * @throws SecurityException {@inheritDoc}
     */
    @Override
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //检查线程是否允许执行Shutdown
            checkShutdownAccess();
            //设置池状态为：SHUTDOWN
            advanceRunState(SHUTDOWN);
            //中断一个可能正在等待任务的线程（只检查第一个线程-不一定标记中断成功）
            interruptIdleWorkers();
            //钩子方法
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        //因为可能终止，所以尝试转换到终止状态
        tryTerminate();
    }

    /**
     * 尝试停止所有正在执行的任务，停止等待任务的处理，并返回等待执行的任务列表。从此方法返回时，将从任务队列中抽取(删除)这些任务。
     * 此方法不等待正在执行的任务终止。使用{@link #awaitTermination awaitTermination}来完成此操作。
     * 除了尽力尝试停止处理正在积极执行的任务之外，没有其他保证。这个实现通过{@link Thread#interrupt}取消任务，因此任何无法响应中断的任务都可能永远不会终止。
     *
     * @throws SecurityException {@inheritDoc}
     */
    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //检查线程是否允许执行Shutdown
            checkShutdownAccess();
            //设置池状态为：STOP
            advanceRunState(STOP);
            //中断所有线程，即使是活动的
            interruptWorkers();
            //将任务队列排空到一个新列表中
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        //尝试终止
        tryTerminate();
        return tasks;
    }

    @Override
    public boolean isShutdown() {
        return !isRunning(ctl.get());
    }

    /**
     * 如果执行器在{@link #shutdown}或{@link #shutdownNow}之后正在终止但没有完全终止，则返回true。
     * 此方法可能用于调试。如果返回{@code true}报告关闭后有足够的时间，
     * 可能表明提交的任务忽略或抑制了中断，从而导致执行器不能正确终止。
     *
     * @return {@code true} 终止中但尚未终止
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return !isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    @Override
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (; ; ) {
                if (runStateAtLeast(ctl.get(), TERMINATED)) {
                    return true;
                }
                if (nanos <= 0) {
                    return false;
                }
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 当这个执行器不再被引用并且它没有线程时调用{@code shutdown}。
     */
    @Override
    protected void finalize() {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null || acc == null) {
            shutdown();
        } else {
            PrivilegedAction<Void> pa = () -> {
                shutdown();
                return null;
            };
            AccessController.doPrivileged(pa, acc);
        }
    }

    /**
     * 返回用于创建新线程的线程工厂。
     *
     * @return 当前线程工厂
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * 设置用于创建新线程的线程工厂。
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
        this.threadFactory = threadFactory;
    }

    /**
     * 返回不可执行任务的当前处理程序。
     *
     * @return 目前的处理程序
     * @see #setRejectedExecutionHandler(RwRejectedExecutionHandler)
     */
    public RwRejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * 为不可执行的任务设置一个新的处理程序。
     *
     * @param handler 新处理程序
     * @throws NullPointerException 如果handler为null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RwRejectedExecutionHandler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        this.handler = handler;
    }

    /**
     * 返回线程的核心数量。
     *
     * @return 线程的核心数量
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * 注：利用次方法，可动态改变核心线程数
     *
     * 设置线程的核心数量。这将覆盖构造函数中设置的任何值。如果新值小于当前值，多余的现有线程将在下一次空闲时终止。如果较大，则需要启动新线程来执行任何排队的任务。
     *
     * @param corePoolSize 新核心线程数
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize) {
            interruptIdleWorkers();
        } else if (delta > 0) {
            /*
             * 我们并不真正知道“需要”多少新线程。作为一种启发，预先启动足够多的新worker(直到新的核心大小)来处理队列中当前的任务数量
             * ，但如果在这样做时队列变为空，则停止。
             */
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty()) {
                    break;
                }
            }
        }
    }

    /**
     * 启动一个核心线程，使其无所事事地等待工作。这将覆盖只有在执行新任务时才启动核心线程的默认策略。
     * 如果所有核心线程都已经启动，这个方法将返回{@code false}。
     *
     * @return {@code true} 如果线程已经启动
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize && addWorker(null, true);
    }

    /**
     * 与prestartCoreThread相同，只是在corePoolSize为0时至少启动一个线程。
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize) {
            addWorker(null, true);
        } else if (wc == 0) {
            addWorker(null, false);
        }
    }

    /**
     * 启动所有核心线程，使它们空闲地等待工作。这将覆盖只有在执行新任务时才启动核心线程的默认策略。（预启动）
     *
     * @return 启动的线程数
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true)) {
            ++n;
        }
        return n;
    }

    /**
     * 如果该池允许核心线程超时，并在保持活动时间内没有任务到达时终止，则返回true，如果需要，则在新任务到达时替换。
     * 当为true时，应用于非核心线程的相同保持活动策略也适用于核心线程。当为false(默认值)时，核心线程不会因为缺少传入任务而终止。
     *
     * @return {@code true} 如果核线程允许超时 否则 {@code false}
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * 设置控制核心线程是否会超时并在保持活动时间内没有任务到达时终止的策略，并在新任务到达时进行替换。
     * 当为false时，核心线程不会因为缺少传入任务而终止。当为true时，应用于非核心线程的相同保持活动策略也适用于核心线程。
     * 为了避免线程持续替换，在设置{@code true}时，保持活动的时间必须大于零。通常应该在积极使用池之前调用此方法。
     *
     * @param value {@code true} 如果应该超时, 否则 {@code false}
     * @throws IllegalArgumentException 如果值为{@code true}且当前保持活动的时间不大于零
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value) {
                interruptIdleWorkers();
            }
        }
    }

    /**
     * 返回允许的最大线程数
     *
     * @return 允许的最大线程数
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * 设置允许的最大线程数。这将覆盖构造函数中设置的任何值。如果新值小于当前值，多余的现有线程将在下一次空闲时终止。
     *
     * @param maximumPoolSize 新的最大
     * @throws IllegalArgumentException 如果新的最大值小于或等于零，或者小于 {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException();
        }
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize) {
            interruptIdleWorkers();
        }
    }

    /**
     * 设置线程在终止之前保持空闲状态的时间限制。如果当前池中线程的数量超过了核心数量，那么在等待这段时间而不处理任务之后，多余的线程将被终止。这将覆盖构造函数中设置的任何值。
     *
     * @param time 等待的时间.  时间值为0将导致多余线程在执行任务后立即终止。
     * @param unit 参数{@code time}的时间单位
     * @throws IllegalArgumentException 如果{@code时间}小于零或
     *                                  如果{@code时间}为零，并且{@code allowsCoreThreadTimeOut}（注意）
     * @see #getKeepAliveTime(TimeUnit)
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0) {
            throw new IllegalArgumentException();
        }
        if (time == 0 && allowsCoreThreadTimeOut()) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0) {
            interruptIdleWorkers();
        }
    }

    /**
     * 返回线程保持活动的时间，这是超过核心池大小的线程在终止之前可能保持空闲的时间。
     *
     * @param unit 结果所需的时间单位
     * @return 时间限制
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /**
     * 返回此执行器使用的任务队列。对任务队列的访问主要用于调试和监视。此队列可能处于活动状态。检索任务队列不会阻止已排队的任务执行。
     *
     * @return 任务队列
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /* 用户级队列公用事业 */

    /**
     * 如果该任务存在，则从执行程序的内部队列中删除该任务，从而导致在尚未启动时不运行该任务。
     *
     * 这种方法作为取消方案的一部分可能有用。在将任务放入内部队列之前，可能无法删除已转换为其他形式的任务。
     * 例如，使用{@code submit}输入的任务可能被转换为维护{@code Future}状态的表单。
     * 然而，在这种情况下，方法{@link #purge}可用于删除那些已取消的
     *
     * @param task 要删除的任务
     * @return {@code true}如果任务被删除
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }

    /**
     * 尝试从工作队列中删除所有已取消的{@link Future}任务。此方法可用于存储回收操作，对功能没有其他影响。
     * 已取消的任务永远不会执行，但可能会在工作队列中累积，直到工作线程可以主动删除它们为止。
     * 而调用此方法则尝试立即删除它们。但是，如果有其他线程的干扰，此方法可能无法删除任务。
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    it.remove();
                }
            }
        } catch (ConcurrentModificationException fallThrough) {
            /*
             * 如果在穿越过程中遇到干扰，选择慢行路径。
             * 复制遍历，调用删除已取消的条目。
             * 缓慢的路径更可能是O(N*N)。
             */
            for (Object r : q.toArray()) {
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    q.remove(r);
                }
            }
        }

        tryTerminate(); // In case SHUTDOWN and now empty
    }

    /**
     * 返回池中的当前线程数。
     *
     * @return 线程的数量
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //排除罕见和惊人的可能性，关于（isTerminated() && getPoolSize() > 0）
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /* 统计数据 */

    /**
     * 返回正在积极执行任务的线程的大致数目。（活动线程数）
     *
     * @return 线程的数量
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (RwThreadPoolExecutor.Worker w : workers) {
                if (w.isLocked()) {
                    ++n;
                }
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回池中曾经同时存在的最大数量的线程。
     *
     * @return 线程的数量
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回曾经计划执行的任务的大致总数。由于任务和线程的状态在计算过程中可能会动态变化，所以返回值只是一个近似值。
     *
     * @return 任务的数量
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (RwThreadPoolExecutor.Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked()) {
                    ++n;
                }
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回已完成执行的任务的大致总数。因为任务和线程的状态可能在计算期间动态变化，所以返回值只是一个近似值，但在连续调用期间不会减少。
     *
     * @return 任务的数量
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (RwThreadPoolExecutor.Worker w : workers) {
                n += w.completedTasks;
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回标识此池及其状态的字符串，包括运行状态指示以及估计的工作人员和任务计数。
     *
     * @return 标识此池及其状态的字符串
     */
    @Override
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (RwThreadPoolExecutor.Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked()) {
                    ++nactive;
                }
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" :
                (runStateAtLeast(c, TERMINATED) ? "Terminated" :
                        "Shutting down"));
        return super.toString() +
                "[" + rs +
                ", pool size = " + nworkers +
                ", active threads = " + nactive +
                ", queued tasks = " + workQueue.size() +
                ", completed tasks = " + ncompleted +
                "]";
    }

    /**
     * 在给定线程中执行给定可运行程序之前调用的方法。此方法由线程{@code t}调用，它将执行task {@code r}，并可用于重新初始化线程局部变量或执行日志记录。
     *
     * 这个实现什么都不做，但是可以在子类中定制。注意:为了正确嵌套多个覆盖，子类通常应该调用{@code super。beforeExecute}在这个方法的末尾。
     *
     * @param t 将运行task {@code r}的线程
     * @param r 将要执行的任务
     */
    protected void beforeExecute(Thread t, Runnable r) {
    }

    /* 钩子方法，类似Spring中的Aware */

    /**
     * 在给定的可运行程序执行完成时调用的方法。此方法由执行任务的线程调用。如果非空，可Throwable是未捕获的导致执行突然终止的{@code RuntimeException}或{@code Error}。
     *
     * 这个实现什么都不做，但是可以在子类中定制。注意:为了正确嵌套多个覆盖，子类通常应该调用{@code super。在这个方法开始的时候}。
     *
     * 注意:当动作被显式或通过{@code submit}等方法包含在任务中(如{@link FutureTask})时，这些任务对象捕获并维护计算异常，因此它们不会导致突然终止，内部异常<em>not</em>传递给该方法。
     * 如果你想在这个方法中捕获这两种失败，你可以进一步探测这种情况，比如在这个示例子类中，如果一个任务被中止，它会打印直接原因或底层异常:
     * <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null && r instanceof Future<?>) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // ignore/reset
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r 已完成的可运行程序
     * @param t 导致终止的异常，如果执行正常完成，则为空
     */
    protected void afterExecute(Runnable r, Throwable t) {
    }

    /**
     * 在执行程序终止时调用的方法。默认实现不做任何事情。注意:为了正确嵌套多个覆盖，子类通常应该调用{@code super。在此方法中终止}。
     */
    protected void terminated() {
    }

    /**
     * 一个用于被拒绝任务的处理程序，它在{@code execute}方法的调用线程中直接运行被拒绝的任务，除非执行器已经关闭，在这种情况下任务将被丢弃。
     */
    public static class CallerRunsPolicy implements RwRejectedExecutionHandler {
        public CallerRunsPolicy() {
        }

        /**
         * 在调用者的线程中执行task r，除非执行器已经关闭，在这种情况下任务将被丢弃。
         *
         * @param r 请求执行的可运行任务
         * @param e 试图执行此任务的执行程序
         */
        @Override
        public void rejectedExecution(Runnable r, RwThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }


    /* 预定义RejectedExecutionHandlers */
    /**
     * 一个被拒绝任务的处理程序，该处理程序引发 {@code RejectedExecutionException}.
     */
    public static class AbortPolicy implements RwRejectedExecutionHandler {
        public AbortPolicy() {
        }

        /**
         * 抛 RejectedExecutionException 异常
         *
         * @param r 请求执行的可运行任务
         * @param e 试图执行此任务的执行程序
         * @throws RejectedExecutionException always
         */
        @Override
        public void rejectedExecution(Runnable r, RwThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                    " rejected from " +
                    e.toString());
        }
    }

    /**
     * A handler for rejected tasks that silently discards the
     * rejected task.
     */
    public static class DiscardPolicy implements RwRejectedExecutionHandler {
        public DiscardPolicy() {
        }

        /**
         * 什么也不做，这会导致丢弃task r。
         *
         * @param r 请求执行的可运行任务
         * @param e 试图执行此任务的执行程序
         */
        @Override
        public void rejectedExecution(Runnable r, RwThreadPoolExecutor e) {
        }
    }

    /**
     * 一个被拒绝任务的处理程序，它丢弃最旧的未处理请求，
     * 然后重试{@code execute}，除非执行程序被关闭，在这种情况下任务被丢弃。
     */
    public static class DiscardOldestPolicy implements RwRejectedExecutionHandler {
        public DiscardOldestPolicy() {
        }

        /**
         * 获取并忽略执行器将以其他方式执行的下一个任务(如果该任务立即可用)，然后重试执行任务r，除非执行器被关闭(在这种情况下，任务r被丢弃)。
         *
         * @param r 请求执行的可运行任务
         * @param e 试图执行此任务的执行程序
         */
        @Override
        public void rejectedExecution(Runnable r, RwThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }

    /**
     * Class Worker主要维护线程运行任务的中断控制状态，以及其他次要的簿记工作。
     * 这个类很有机会扩展了AbstractQueuedSynchronizer以简化获取和释放围绕每个任务执行的锁。
     * 这样可以防止中断正在运行的任务，而不是唤醒等待任务的工作线程。我们实现了一个简单的不可重入互斥锁
     * ，而不是使用ReentrantLock，因为我们不希望工作任务在调用诸如setCorePoolSize之类的池控制方法时能够重新获得锁。
     * 此外，为了在线程真正开始运行任务之前抑制中断，我们将锁状态初始化为负值，并在启动时清除它(在runWorker中)。
     * 注：Worker继承了AQS
     */
    private final class Worker extends RwAbstractQueuedSynchronizer implements Runnable {
        /**
         * 这个类永远不会被序列化，但是我们提供了一个serialVersionUID来阻止javac警告。
         */
        private static final long serialVersionUID = 6138294804551838833L;

        /**
         * 线程这个worker正在运行。如果工厂失败，则为空。
         */
        final Thread thread;
        /**
         * 要运行的初始任务。可能是空。
         */
        Runnable firstTask;
        /**
         * 线程任务计数器（已完成数量）
         */
        volatile long completedTasks;

        /**
         * 从ThreadFactory用给定的第一个任务和线程创建。
         * @param firstTask 第一个任务(如果没有，则为空)
         */
        Worker(Runnable firstTask) {
            //在运行worker之前禁止中断
            setState(-1);
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        /**
         * 将主运行循环委托给外部运行worker
         */
        @Override
        public void run() {
            runWorker(this);
        }

        /**
         * 锁的方法
         * 值0表示未解锁状态。
         * 值1表示锁定状态。
         */
        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        @Override
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        public void unlock() {
            release(1);
        }

        public boolean isLocked() {
            return isHeldExclusively();
        }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }
}
