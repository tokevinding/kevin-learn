package com.kevin.threads.juc.aqs.rw;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * @author kevin
 * @date 2020-10-23 14:25:26
 * @desc
 */
public abstract class RwAbstractQueuedSynchronizer extends RwAbstractOwnableSynchronizer implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * 创建一个初始同步状态为零的新{@code AbstractQueuedSynchronizer}实例。
     */
    protected RwAbstractQueuedSynchronizer() { }

    /**
     * 等待队列节点类。
     *
     * 等待队列是“CLH”(Craig、Landin和Hagersten)锁队列的变体。CLH锁通常用于自旋锁。
     * 相反，我们使用它们来阻塞同步器，但使用相同的基本策略，即在线程的前任节点中保存关于线程的一些控制信息。
     * 每个节点中的“状态”字段跟踪线程是否应该阻塞。当一个节点的前任释放时，它会被告知。队列的每个节点充当一个特定通知样式的监视器，
     * 其中包含一个等待线程。状态字段并不控制线程是否被授予锁等等。如果线程是队列中的第一个线程，它可能会尝试获取。
     * 但做第一并不能保证成功; 它只给人奋斗的权利。因此当前释放的竞争者线程可能需要重新等待。
     *
     * <p>要进入CLH锁队列，需要将其作为新的tail原子地拼接。要退出队列，只需设置头部字段。
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>插入到CLH队列只需要对“tail”执行一个原子操作，因此从未排队到排队有一个简单的原子点划分。
     * 类似地，退出队列只涉及更新“头部”。但是，节点需要多做一些工作来确定谁是它们的后继节点，部分原因是要处理由于超时和中断而可能导致的取消。
     *
     * “prev”链接(在原始CLH锁中不使用)主要用于处理取消。如果一个节点被取消，它的后续节点(通常)会重新连接到一个未取消的前任节点。
     * 关于自旋锁的类似力学解释，请参阅Scott和Scherer的论文http://www.cs.rochester.edu/u/scott/synchronization/
     *
     * 我们还使用“next”链接来实现阻塞机制。每个节点的线程id保存在它自己的节点中，因此前一个节点通过遍历下一个链接来确定它是哪个线程，从而向下一个节点发出唤醒信号。
     * 确定继承节点必须避免与新排队的节点竞争，以设置它们的前任的“下一个”字段。当一个节点的后继为空时，通过从原子更新的“tail”向后检查，可以解决这个问题。
     * (或者，换句话说，next-links是一种优化，因此我们通常不需要向后扫描。)
     *
     * <p>对消在基本算法中引入了一些保守性。由于我们必须轮询是否取消了其他节点，因此我们可能无法注意到被取消的节点是在我们的前面还是后面。
     * 解决这个问题的方法是在取消时始终取消继任者，允许他们在新的前任上稳定下来，除非我们能够确定一个未取消的前任将承担这一责任。
     *
     * <p>CLH队列需要一个虚拟头节点来启动。但我们不是在建设中创造它们，因为如果没有争用，那就是白费力气。相反，在第一次争用时构造节点并设置头和尾指针。
     *
     * <p>等待条件的线程使用相同的节点，但使用额外的链接。条件只需要链接简单(非并发)链接队列中的节点，因为只有在独占持有时才会访问它们。
     * 在等待时，节点被插入到条件队列中。收到信号后，节点被传输到主队列。status字段的一个特殊值用于标记节点所在的队列。
     *
     * <p>感谢Dave Dice、Mark Moir、Victor Luchangco、Bill Scherer和Michael Scott，以及JSR-166专家组的成员们，对这门课的设计提出有帮助的想法、讨论和评论。
     */
    static final class Node {
        /** 指示节点在共享模式下等待的标记 */
        static final RwAbstractQueuedSynchronizer.Node SHARED = new RwAbstractQueuedSynchronizer.Node();
        /** 指示节点以排他模式等待的标记 */
        static final RwAbstractQueuedSynchronizer.Node EXCLUSIVE = null;

        /** waitStatus值表示线程已被取消 */
        static final int CANCELLED =  1;
        /** waitStatus值，表示后续线程需要解除停车(unpark) */
        static final int SIGNAL    = -1;
        /** waitStatus值表示线程正在等待状态 */
        static final int CONDITION = -2;
        /**
         * 表示下一个默认的应该无条件传播的等待状态值
         */
        static final int PROPAGATE = -3;

        /**
         * 状态字段，只接受值:
         *   SIGNAL:     这个节点的后继被(或即将)阻塞(通过park)，因此当前节点在释放或取消时必须释放它的后继。
         *               为了避免竞争，acquire方法必须首先表明它们需要一个信号，然后重试原子获取，当失败时，阻塞。
         *   CANCELLED:  由于超时或中断，该节点被取消。节点不会离开这个状态。特别是，取消节点的线程不会再次阻塞。
         *   CONDITION:  此节点当前处于条件队列中。在传输之前，它不会被用作同步队列节点，此时状态将被设置为0。(这里使用这个值与该场的其他用途无关，但简化了力学。)
         *   PROPAGATE:  释放的共享应该传播到其他节点。在doReleaseShared中设置这个(仅针对头节点)，以确保传播继续，即使其他操作已经干预。
         *   0:          以上都不是
         *
         * 数值按数值排列以简化使用。非负值意味着节点不需要信号。
         * 所以，大多数代码不需要检查特定的值，只需要检查符号。
         *
         * 对于普通同步节点，该字段被初始化为0，对于条件节点，该字段被初始化为条件。
         * 使用CAS修改它(或者在可能的情况下，无条件的volatile写操作)。
         */
        volatile int waitStatus;

        /**
         * 链接到当前节点/线程检查等待状态所依赖的前任节点。在排队期间分配，只有在退出队列时才为空(为了GC)。
         * 另外，在取消一个前任节点时，我们在寻找一个未取消的节点时短路，因为头节点从未被取消过，所以这个头节点始终存在:只有在成功获取后，
         * 一个节点才会成为头节点。被取消的线程永远不会成功获取，并且线程只取消自己，而不取消任何其他节点。
         */
        volatile RwAbstractQueuedSynchronizer.Node prev;

        /**
         * 链接到当前节点/线程在释放时释放的后继节点。在排队期间赋值，在绕过取消的前任时进行调整，在退出队列时为空(为了GC)。
         * 直到附件之后，enq操作才会分配一个前任的下一个字段，因此看到一个空的下一个字段并不一定意味着节点在队列的末尾。
         * 但是，如果下一个字段看起来是null，我们可以从尾部扫描prev来再次检查。
         * 已取消节点的下一个字段被设置为指向节点本身，而不是null，以使isOnSyncQueue更加容易。
         */
        volatile RwAbstractQueuedSynchronizer.Node next;

        /**
         * 使该节点进入队列的线程。在构造时初始化，使用后为空。
         */
        volatile Thread thread;

        /**
         * 链接到下一个等待状态的节点，或共享特殊值。因为条件队列只有在以排他模式持有时才会被访问，
         * 所以我们只需要一个简单的链接队列来在节点等待条件时持有它们。然后将它们传输到队列中重新获取。
         * 因为条件只能是排他的，所以我们通过使用特殊值来表示共享模式来保存字段。
         */
        RwAbstractQueuedSynchronizer.Node nextWaiter;

        /**
         * 如果节点在共享模式中等待，则返回true。
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * 返回前一个节点，如果为空则抛出NullPointerException。
         * 当前任不能为空时使用。空检查可以省略，但存在是为了帮助VM。
         *
         * @return 此节点的前身
         */
        final RwAbstractQueuedSynchronizer.Node predecessor() throws NullPointerException {
            RwAbstractQueuedSynchronizer.Node p = prev;
            if (p == null) {
                throw new NullPointerException();
            } else {
                return p;
            }
        }

        //用于建立初始头或共享标记
        Node() {
        }

        Node(Thread thread, RwAbstractQueuedSynchronizer.Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * 等待队列的头，被惰性地初始化。除了初始化之外，它只通过方法setHead进行修改。
     * 注意:如果head存在，它的等待状态保证不会被取消。
     */
    private transient volatile RwAbstractQueuedSynchronizer.Node head;

    /**
     * 等待队列的尾部，延迟初始化。仅通过方法enq修改以添加新的等待节点。
     */
    private transient volatile RwAbstractQueuedSynchronizer.Node tail;

    /**
     * 同步状态。
     *
     * ReentrantLock.NonfairSync表示：
     * 1.当state=0时，表示无锁状态
     * 2.当state>0时，表示已经有线程获得了锁，也就是state=1，但是因为ReentrantLock允许重入
     * ，所以同一个线程多次获得同步锁的时候，state会递增，比如重入5次，那么state=5。
     * 而在释放锁的时候，同样需要释放5次直到state=0其他线程才有资格获得锁
     *
     */
    private volatile int state;

    /**
     * 返回同步状态的当前值。该操作具有{@code volatile} read的内存语义。
     * @return 当前状态值
     */
    protected final int getState() {
        return state;
    }

    /**
     * 设置同步状态的值。该操作具有{@code volatile}写入的内存语义。
     * @param newState 新状态值
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * 如果当前状态值等于预期值，则自动将同步状态设置为给定的更新值。该操作具有{@code volatile}读写的内存语义。
     *
     * @param expect 期望值
     * @param update 新值
     * @return {@code true} 如果成功。False return表示实际值与预期值不相等。
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // 请参阅下面的intrinsics设置以支持这一点
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    /**
     * 旋转比计时停车快的纳秒数。一个粗略的估计就足以在很短的超时时间内提高响应能力。
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * 将节点插入队列，必要时进行初始化。
     * 特殊分析：
     * 假如有两个线程t1,t2同时进入enq方法，t==null表示队列是首次使用，需要先初始化，另外一个线程cas失败，则进入下次循环，通过cas操作将node添加到队尾
     * @param node 要插入的节点
     * @return 节点的前任
     */
    private RwAbstractQueuedSynchronizer.Node enq(final RwAbstractQueuedSynchronizer.Node node) {
        //自旋
        for (;;) {
            RwAbstractQueuedSynchronizer.Node t = tail;
            //如果是第一次添加到队列，那么tail=null
            if (t == null) { // Must initialize
                //初始化
                if (compareAndSetHead(new RwAbstractQueuedSynchronizer.Node())) {
                    //此时队列中只一个头结点，所以tail也指向它
                    tail = head;
                }
            } else {
                //进行第二次循环时，tail不为null，进入else区域。将当前线程的Node结点的prev指向tail，然后使用CAS将tail指向Node
                node.prev = t;
                //已初始化-通过cas将node添加到AQS队列
                //t此时指向tail,所以可以CAS成功，将tail重新指向Node。此时t为更新前的tail的值，即指向空的头结点，t.next=node，就将头结点的后续结点指向Node，返回头结点
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    /**
     * 为当前线程和给定模式创建和排队节点。
     *
     * 说明：
     * .将当前线程封装成Node
     * .判断当前链表中的tail节点是否为空，如果不为空，则通过cas操作把当前线程的node添加到AQS队列
     * .如果为空或者cas失败，调用enq将节点添加到AQS队列
     *
     * @param mode Node.EXCLUSIVE for 排他, Node.SHARED for 共享
     * @return 新节点
     */
    private RwAbstractQueuedSynchronizer.Node addWaiter(RwAbstractQueuedSynchronizer.Node mode) {
        //将当前线程封装成Node
        RwAbstractQueuedSynchronizer.Node node = new RwAbstractQueuedSynchronizer.Node(Thread.currentThread(), mode);
        // 试试enq的快速路径;失败时备份到完整的enq
        //// tail是AQS的中表示同步队列队尾的属性，刚开始为null，所以进行enq(node)方法
        RwAbstractQueuedSynchronizer.Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            //通过cas将node添加到AQS队列
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        //tail=null，将node添加到同步队列中
        enq(node);
        return node;
    }

    /**
     * 将队列头设置为节点，从而退出队列。仅由获取方法调用。另外，为了进行GC和抑制不必要的信号和遍历，还将未使用的字段设为空。
     *
     * @param node 节点
     */
    private void setHead(RwAbstractQueuedSynchronizer.Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * 唤醒节点的后继(如果存在)。
     *
     * @param node the node
     */
    private void unparkSuccessor(RwAbstractQueuedSynchronizer.Node node) {
        /*
         * 如果状态是消极的(例如，可能需要信号)，试着在信号的预期中清除。如果失败或者状态被等待的线程改变了，也没有问题。
         */
        int ws = node.waitStatus;
        if (ws < 0) {
            compareAndSetWaitStatus(node, ws, 0);
        }

        /*
         * 要unpark的线程被保存在后继节点中，后继节点通常就是下一个节点。
         * 但如果取消或明显为空，从尾部向后遍历以找到实际的未取消的后继。
         */
        RwAbstractQueuedSynchronizer.Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (RwAbstractQueuedSynchronizer.Node t = tail; t != null && t != node; t = t.prev) {
                if (t.waitStatus <= 0) {
                    s = t;
                }
            }
        }
        if (s != null) {
            LockSupport.unpark(s.thread);
        }
    }

    /**
     * 共享模式的释放操作——向后继发送信号并确保传播。
     * (注:对于独占模式，release相当于在head需要信号时调用unpark后继。)
     */
    private void doReleaseShared() {
        /*
         * 确保发布传播，即使有其他正在进行的获取/发布。这是按照通常的方式进行的，即在head需要信号的时候尝试解锁它的继任者。
         * 但如果不这样做，则将状态设置为PROPAGATE，以确保在发布时继续传播。此外，在执行此操作时，我们必须进行循环，以防添加了新节点。
         * 另外，与unpark继任人的其他使用不同，我们需要知道CAS重置状态是否失败，如果失败，需要重新检查。
         */
        for (;;) {
            RwAbstractQueuedSynchronizer.Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                //头节点的waitStatus为SIGNAL，则需要执行unpark头节点
                if (ws == RwAbstractQueuedSynchronizer.Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, RwAbstractQueuedSynchronizer.Node.SIGNAL, 0)) {
                        continue;            // loop to recheck cases
                    }
                    unparkSuccessor(h);
                }
                //头节点的等待状态是0，并且设置头节点的waitStatus 为 无条件传播的等待
                else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, RwAbstractQueuedSynchronizer.Node.PROPAGATE)) {
                    continue;                // loop on failed CAS
                }
            }

            if (h == head)                   // loop if head changed
            {
                break;
            }
        }
    }

    /**
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating if either propagate > 0 or
     * PROPAGATE status was set.
     *
     * @param node the node
     * @param propagate the return value from a tryAcquireShared
     */
    private void setHeadAndPropagate(RwAbstractQueuedSynchronizer.Node node, int propagate) {
        RwAbstractQueuedSynchronizer.Node h = head; // Record old head for check below
        setHead(node);
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
                (h = head) == null || h.waitStatus < 0) {
            RwAbstractQueuedSynchronizer.Node s = node.next;
            if (s == null || s.isShared())
                doReleaseShared();
        }
    }

    // Utilities for various versions of acquire

    /**
     * Cancels an ongoing attempt to acquire.
     *
     * @param node the node
     */
    private void cancelAcquire(RwAbstractQueuedSynchronizer.Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;

        node.thread = null;

        // Skip cancelled predecessors
        RwAbstractQueuedSynchronizer.Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        RwAbstractQueuedSynchronizer.Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        node.waitStatus = RwAbstractQueuedSynchronizer.Node.CANCELLED;

        // If we are the tail, remove ourselves.
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            int ws;
            if (pred != head &&
                    ((ws = pred.waitStatus) == RwAbstractQueuedSynchronizer.Node.SIGNAL ||
                            (ws <= 0 && compareAndSetWaitStatus(pred, ws, RwAbstractQueuedSynchronizer.Node.SIGNAL))) &&
                    pred.thread != null) {
                RwAbstractQueuedSynchronizer.Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node);
            }

            node.next = node; // help GC
        }
    }

    /**
     * 检查和更新未能获取的节点的状态。如果线程应该阻塞返回真。这是所有采集回路中的主要信号控制。需要pred == node.prev。
     *
     * @param pred node's predecessor holding status
     * @param node the node
     * @return {@code true} if thread should block
     */
    private static boolean shouldParkAfterFailedAcquire(RwAbstractQueuedSynchronizer.Node pred, RwAbstractQueuedSynchronizer.Node node) {
        int ws = pred.waitStatus;
        if (ws == RwAbstractQueuedSynchronizer.Node.SIGNAL) {
            //这个节点已经设置了状态，要求释放信号给它，这样它就可以安全停车了。
            return true;
        }
        if (ws > 0) {
            /*
             * 前任被取消了。跳过前辈并指示重试。
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * waitStatus <= 0(CANCELLED 或 为0)，设置head 的 waitStatus = -1（SIGNAL，后续需要unpark）
             * 等待状态必须为0或传播。指示我们需要信号，但先别停车。来电者需重试以确保停车前无法取得。
             */
            compareAndSetWaitStatus(pred, ws, RwAbstractQueuedSynchronizer.Node.SIGNAL);
        }
        return false;
    }

    /**
     * 方便其他方法中断当前线程。
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * 方便方法park后检查是否中断
     *
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        System.out.println("parkAndCheckInterrupt - 线程"+ Thread.currentThread().getName()+"park");
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     */

    /**
     * 以排他不可中断模式获取已经在队列中的线程。
     * 由条件等待方法和获取方法使用。
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} 如果在等待时中断
     */
    final boolean acquireQueued(final RwAbstractQueuedSynchronizer.Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                System.out.println("acquireQueued - 线程"+ Thread.currentThread().getName()+"正在获取锁");
                final RwAbstractQueuedSynchronizer.Node p = node.predecessor();
                // 如果前驱为head才有资格进行锁的抢夺
                if (p == head && tryAcquire(arg)) {
                    // 获取锁成功后就不需要再进行同步操作了,获取锁成功的线程作为新的head节点
                    //凡是head节点,head.thread与head.prev永远为null, 但是head.next不为null
                    setHead(node);
                    p.next = null; // help GC
                    //获取锁成功
                    failed = false;
                    return interrupted;
                }
                //如果获取锁失败，则根据节点的waitStatus决定是否需要挂起线程
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in exclusive interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireInterruptibly(int arg)
            throws InterruptedException {
        final RwAbstractQueuedSynchronizer.Node node = addWaiter(RwAbstractQueuedSynchronizer.Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final RwAbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in exclusive timed mode.
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final RwAbstractQueuedSynchronizer.Node node = addWaiter(RwAbstractQueuedSynchronizer.Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final RwAbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                        nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared uninterruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireShared(int arg) {
        final RwAbstractQueuedSynchronizer.Node node = addWaiter(RwAbstractQueuedSynchronizer.Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final RwAbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
            throws InterruptedException {
        final RwAbstractQueuedSynchronizer.Node node = addWaiter(RwAbstractQueuedSynchronizer.Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final RwAbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared timed mode.
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final RwAbstractQueuedSynchronizer.Node node = addWaiter(RwAbstractQueuedSynchronizer.Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final RwAbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                        nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // Main exported methods

    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 尝试设置状态以反映排他模式下的发布。
     *
     * <p>这个方法总是由执行释放的线程调用。
     *
     * <p>默认实现抛出{@link UnsupportedOperationException}.
     *
     * @param arg 释放的论点。此值始终是传递给release方法的值，或进入条件等待时的当前状态值。
     *            否则，该值是未解释的，可以表示您喜欢的任何内容。
     * @return {@code true} 如果该对象现在处于完全释放状态，那么任何等待的线程都可以尝试获取;否则为{@code false}。
     * @throws IllegalMonitorStateException
     *            如果释放，则该同步器将处于非法状态。必须以一致的方式抛出此异常，才能使同步正常工作。
     * @throws UnsupportedOperationException 如果不支持排他模式
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 尝试在共享模式中获取。此方法应该查询对象的状态是否允许在共享模式下获取它，如果允许，则获取它。
     *
     * <p>这个方法总是由执行acquire的线程调用。如果这个方法报告失败
     * ，那么获取方法可能会让线程排队(如果它还没有排队)，直到其他线程释放它为止。
     *
     * <p>默认实现抛出{@link UnsupportedOperationException}。
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return a negative value on failure; zero if acquisition in shared
     *         mode succeeded but no subsequent shared-mode acquire can
     *         succeed; and a positive value if acquisition in shared
     *         mode succeeded and subsequent shared-mode acquires might
     *         also succeed, in which case a subsequent waiting thread
     *         must check availability. (Support for three different
     *         return values enables this method to be used in contexts
     *         where acquires only sometimes act exclusively.)  Upon
     *         success, this object has been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in shared mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a non-waiting {@link RwAbstractQueuedSynchronizer.ConditionObject} method.
     * (Waiting methods instead invoke {@link #release}.)
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link RwAbstractQueuedSynchronizer.ConditionObject} methods, so need
     * not be defined if conditions are not used.
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * 译:获取处于独占模式，忽略中断。通过调用至少一次{@link #tryAcquire}来实现，成功后返回。
     * 否则，线程会排队，可能会反复阻塞和解除阻塞，调用{@link #tryAcquire}直到成功。
     * 这个方法可以用来实现方法{@link Lock# Lock}。
     *
     * @param arg 获取参数。这个值被传递给{@link #tryAcquire}，但是没有被解释，可以表示任何你喜欢的内容。
     */
    public final void acquire(int arg) {
        /*
         * 方法的主要逻辑:
         * 1.通过tryAcquire尝试获取独占锁，如果成功返回true，失败返回false
         * 2.如果tryAcquire失败，则会通过addWaiter方法将当前线程封装成Node添加到AQS队列尾部
         * 3.acquireQueued，将Node作为参数，通过自旋去尝试获取锁。
         */
        if (!tryAcquire(arg) &&
                acquireQueued(addWaiter(RwAbstractQueuedSynchronizer.Node.EXCLUSIVE), arg)) {
            selfInterrupt();
            System.out.println("selfInterrupt done");
        }
//        System.out.println("acquire done");
    }

    /**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            doAcquireInterruptibly(arg);
    }

    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
                doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * 以独占模式发布。如果{@link #tryRelease}返回true，通过解除一个或多个线程的阻塞来实现。
     * 这个方法可以用来实现方法{@link Lock#unlock}。
     *
     * @param arg 释放的论点。这个值被传递给{@link #tryRelease}，但是不被解释，可以表示任何你喜欢的值。
     * @return 从 {@link #tryRelease} 返回的值
     */
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            RwAbstractQueuedSynchronizer.Node h = head;
            if (h != null && h.waitStatus != 0) {
                unparkSuccessor(h);
            }
            return true;
        }
        return false;
    }

    /**
     * 以共享模式获取，忽略中断。
     * 首先至少调用一次{@link # tryacquiremred}，成功后返回。
     * 否则，线程就会排队，可能会反复阻塞和解除阻塞，调用{@link # tryacquiremred}，直到成功。
     *
     * @param arg 获取参数。这个值被传递给{@link # tryacquiremred}，但是不被解释，可以表示任何你喜欢的内容。
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0) {
            doAcquireShared(arg);
        }
    }

    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
                doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         */
        RwAbstractQueuedSynchronizer.Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
                s.prev == head && (st = s.thread) != null) ||
                ((h = head) != null && (s = h.next) != null &&
                        s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         */

        RwAbstractQueuedSynchronizer.Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * Returns true if the given thread is currently queued.
     *
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue
     * @throws NullPointerException if the thread is null
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (RwAbstractQueuedSynchronizer.Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        RwAbstractQueuedSynchronizer.Node h, s;
        return (h = head) != null &&
                (s = h.next)  != null &&
                !s.isShared()         &&
                s.thread != null;
    }

    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    public final boolean hasQueuedPredecessors() {
        // 这取决于head在tail和on head之前被初始化。下一个是准确的，如果当前线程在队列中是第一个。
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        RwAbstractQueuedSynchronizer.Node t = tail; // Read fields in reverse initialization order
        RwAbstractQueuedSynchronizer.Node h = head;
        RwAbstractQueuedSynchronizer.Node s;
        return h != t &&
                ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting to acquire
     */
    public final int getQueueLength() {
        int n = 0;
        for (RwAbstractQueuedSynchronizer.Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (RwAbstractQueuedSynchronizer.Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (RwAbstractQueuedSynchronizer.Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     *
     * @return the collection of threads
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (RwAbstractQueuedSynchronizer.Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
                "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions

    /**
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     * @param node the node
     * @return true if is reacquiring
     */
    final boolean isOnSyncQueue(RwAbstractQueuedSynchronizer.Node node) {
        if (node.waitStatus == RwAbstractQueuedSynchronizer.Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        return findNodeFromTail(node);
    }

    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     * @return true if present
     */
    private boolean findNodeFromTail(RwAbstractQueuedSynchronizer.Node node) {
        RwAbstractQueuedSynchronizer.Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    /**
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     */
    final boolean transferForSignal(RwAbstractQueuedSynchronizer.Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         */
        if (!compareAndSetWaitStatus(node, RwAbstractQueuedSynchronizer.Node.CONDITION, 0))
            return false;

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         */
        RwAbstractQueuedSynchronizer.Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, RwAbstractQueuedSynchronizer.Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     *
     * @param node the node
     * @return true if cancelled before the node was signalled
     */
    final boolean transferAfterCancelledWait(RwAbstractQueuedSynchronizer.Node node) {
        if (compareAndSetWaitStatus(node, RwAbstractQueuedSynchronizer.Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     * @param node the condition node for this wait
     * @return previous sync state
     */
    final int fullyRelease(RwAbstractQueuedSynchronizer.Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed) {
                node.waitStatus = Node.CANCELLED;
            }
        }
    }

    // Instrumentation methods for conditions

    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(RwAbstractQueuedSynchronizer.ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final boolean hasWaiters(RwAbstractQueuedSynchronizer.ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final int getWaitQueueLength(RwAbstractQueuedSynchronizer.ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final Collection<Thread> getWaitingThreads(RwAbstractQueuedSynchronizer.ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    /**
     * Condition implementation for a {@link
     * RwAbstractQueuedSynchronizer} serving as the basis of a {@link
     * Lock} implementation.
     *
     * <p>Method documentation for this class describes mechanics,
     * not behavioral specifications from the point of view of Lock
     * and Condition users. Exported versions of this class will in
     * general need to be accompanied by documentation describing
     * condition semantics that rely on those of the associated
     * {@code AbstractQueuedSynchronizer}.
     *
     * <p>This class is Serializable, but all fields are transient,
     * so deserialized conditions have no waiters.
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** First node of condition queue. */
        private transient RwAbstractQueuedSynchronizer.Node firstWaiter;
        /** Last node of condition queue. */
        private transient RwAbstractQueuedSynchronizer.Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * Adds a new waiter to wait queue.
         * @return its new wait node
         */
        private RwAbstractQueuedSynchronizer.Node addConditionWaiter() {
            RwAbstractQueuedSynchronizer.Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            if (t != null && t.waitStatus != RwAbstractQueuedSynchronizer.Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            RwAbstractQueuedSynchronizer.Node node = new RwAbstractQueuedSynchronizer.Node(Thread.currentThread(), RwAbstractQueuedSynchronizer.Node.CONDITION);
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * @param first (non-null) the first node on condition queue
         */
        private void doSignal(RwAbstractQueuedSynchronizer.Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null) {
                    lastWaiter = null;
                }
                first.nextWaiter = null;
            } while (!transferForSignal(first) &&
                    (first = firstWaiter) != null);
        }

        /**
         * Removes and transfers all nodes.
         * @param first (non-null) the first node on condition queue
         */
        private void doSignalAll(RwAbstractQueuedSynchronizer.Node first) {
            lastWaiter = firstWaiter = null;
            do {
                RwAbstractQueuedSynchronizer.Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * Unlinks cancelled waiter nodes from condition queue.
         * Called only while holding lock. This is called when
         * cancellation occurred during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
         */
        private void unlinkCancelledWaiters() {
            RwAbstractQueuedSynchronizer.Node t = firstWaiter;
            RwAbstractQueuedSynchronizer.Node trail = null;
            while (t != null) {
                RwAbstractQueuedSynchronizer.Node next = t.nextWaiter;
                if (t.waitStatus != RwAbstractQueuedSynchronizer.Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    trail = t;
                t = next;
            }
        }

        // public methods

        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        @Override
        public final void signal() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            RwAbstractQueuedSynchronizer.Node first = firstWaiter;
            if (first != null) {
                doSignal(first);
            }
        }

        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        @Override
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            RwAbstractQueuedSynchronizer.Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            RwAbstractQueuedSynchronizer.Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         */
        private int checkInterruptWhileWaiting(RwAbstractQueuedSynchronizer.Node node) {
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         */
        private void reportInterruptAfterWait(int interruptMode)
                throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * Implements interruptible condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            RwAbstractQueuedSynchronizer.Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            RwAbstractQueuedSynchronizer.Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            RwAbstractQueuedSynchronizer.Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            RwAbstractQueuedSynchronizer.Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(RwAbstractQueuedSynchronizer sync) {
            return sync == RwAbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link RwAbstractQueuedSynchronizer#hasWaiters(RwAbstractQueuedSynchronizer.ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (RwAbstractQueuedSynchronizer.Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == RwAbstractQueuedSynchronizer.Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link RwAbstractQueuedSynchronizer#getWaitQueueLength(RwAbstractQueuedSynchronizer.ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (RwAbstractQueuedSynchronizer.Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == RwAbstractQueuedSynchronizer.Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link RwAbstractQueuedSynchronizer#getWaitingThreads(RwAbstractQueuedSynchronizer.ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (RwAbstractQueuedSynchronizer.Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == RwAbstractQueuedSynchronizer.Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     */
    private static final Unsafe unsafe;//= Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            //获取成员变量
            Field field=Unsafe.class.getDeclaredField("theUnsafe");
            //设置为可访问
            field.setAccessible(true);
            //是静态字段,用null来获取Unsafe实例
            unsafe = (Unsafe)field.get(null);

            stateOffset = unsafe.objectFieldOffset
                    (RwAbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                    (RwAbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                    (RwAbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                    (RwAbstractQueuedSynchronizer.Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                    (RwAbstractQueuedSynchronizer.Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     */
    private final boolean compareAndSetHead(RwAbstractQueuedSynchronizer.Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(RwAbstractQueuedSynchronizer.Node expect, RwAbstractQueuedSynchronizer.Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(RwAbstractQueuedSynchronizer.Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(RwAbstractQueuedSynchronizer.Node node,
                                                   RwAbstractQueuedSynchronizer.Node expect,
                                                   RwAbstractQueuedSynchronizer.Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
