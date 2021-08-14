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
        /** waitStatus值表示线程正在等待状态(条件队列) */
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
        // 代码会执行到这里, 只有两种情况:
        //    1. 队列为空
        //    2. CAS失败
        // 如果当前node插入队尾失败，则通过自旋保证替换成功(自旋+CAS)
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
         * 但如果取消或明显为空，从尾部向前遍历以找到实际的未取消的后继。
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
            //unpark node后面的一个未取消的节点（离node最近的）
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
            //从头节点开始
            RwAbstractQueuedSynchronizer.Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                //头节点的waitStatus为SIGNAL，则需要执行unpark头节点
                if (ws == RwAbstractQueuedSynchronizer.Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, RwAbstractQueuedSynchronizer.Node.SIGNAL, 0)) {
                        continue;            // loop to recheck cases
                    }
                    //唤醒这个节点
                    unparkSuccessor(h);
                }
                //头节点的等待状态是0，并且设置头节点的waitStatus 为 无条件传播的等待
                //不需要唤醒，则CAS设置状态为PROPAGATE，继续循环
                else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, RwAbstractQueuedSynchronizer.Node.PROPAGATE)) {
                    continue;                // loop on failed CAS
                }
            }

            //头结点没有改变，则设置成功，退出循环
            if (h == head)                   // loop if head changed
            {
                break;
            }
        }
    }

    /**
     * 如果设置了 propagate > 0 或 PROPAGATE 状态。
     * 设置队列头，并检查后继是否可能在共享模式中等待设置队列头，并检查后继是否可能在共享模式中等待
     *
     * @param node the node
     * @param propagate the return value from a tryAcquireShared
     */
    private void setHeadAndPropagate(RwAbstractQueuedSynchronizer.Node node, int propagate) {
        RwAbstractQueuedSynchronizer.Node h = head; // Record old head for check below
        //把当前获取到锁的节点设置为头结点
        setHead(node);
        /*
         * 尝试向下一个排队节点发出信号 如果:
         *   传播是由呼叫者指示的,
         *     或被前一个操作记录(作为h.waitStatus在setHead之前或之后)
         *     (注意:这使用了waitStatus的sign-check，因为PROPAGATE状态可能会转换为SIGNAL。)
         * 并且
         *   下一个节点正在共享模式中等待，或者下一个节点是null
         *
         * 这两种检查的保守性可能会导致不必要的唤醒，但只有在有多线程竞争 获取/释放 时，所以大多数现在或很快就需要信号。
         * 为true的场景：
         * 1.当前共享模式中获取成功，后续的共享模式获取也可能成功（propagate大于0表示后面的节点也需要唤醒）
         * 2.原头节点为空
         * 3.原头节点非空 但状态<0
         * 4.新头节点为空
         * 5.新头节点非空 但状态<0
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
     * 取消正在进行的尝试获取
     *
     * @param node the node
     */
    private void cancelAcquire(RwAbstractQueuedSynchronizer.Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;

        node.thread = null;

        // 跳过已取消的前任
        RwAbstractQueuedSynchronizer.Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext显然是要解拼接的节点。
        // 如果不这样，下面的情况将失败，在这种情况下，我们输掉了与另一个取消或信号的比赛，所以不需要进一步的操作。
        RwAbstractQueuedSynchronizer.Node predNext = pred.next;

        // 在这里，可以使用无条件写入代替CAS。
        // 在这个原子步骤之后，其他节点可以跳过我们。以前，我们不受其他线程的干扰。
        node.waitStatus = RwAbstractQueuedSynchronizer.Node.CANCELLED;

        // If we are the tail, remove ourselves.
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            // 如果后继者需要信号，尝试设置pred的下一个链接，这样它就会得到一个。否则，唤醒它传播
            int ws;
            //1.为true的情况：
            //    1.1.前驱非头节点 并且 前驱的等待状态为SIGNAL 并且 前驱的线程非空
            //    1.2.前驱非头节点 并且 前驱的等待状态 <= 0 并且 设置前驱的等待状态(为SIGNAL)成功  并且 前驱的线程非空
            if (pred != head &&
                    ((ws = pred.waitStatus) == RwAbstractQueuedSynchronizer.Node.SIGNAL ||
                            (ws <= 0 && compareAndSetWaitStatus(pred, ws, RwAbstractQueuedSynchronizer.Node.SIGNAL))) &&
                    pred.thread != null) {
                //当前节点有next，则设置前驱的next = 当前节点的next
                RwAbstractQueuedSynchronizer.Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                //唤醒节点的后继(如果存在)
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
     * @return {@code true} 如果线程阻塞返回true
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
             * waitStatus <= 0(非CANCELLED)，设置head 的 waitStatus = -1（SIGNAL，后续需要unpark）
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
     * 方法park后检查是否中断
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
     * 以排他可中断模式获取。
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
                    //如果中断，直接抛出异常（和acquireQueued比较）
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
     * 以共享不间断模式获取。
     * @param arg the acquire argument
     */
    private void doAcquireShared(int arg) {
        final RwAbstractQueuedSynchronizer.Node node = addWaiter(RwAbstractQueuedSynchronizer.Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                //获取前一个节点
                final RwAbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == head) {
                    //尝试获取共享锁
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        //和独占模式不同的地方，会唤醒后面的共享节点
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                //挂起
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
     * 以共享可中断模式获取
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

    //---------------------------------------注意：主要出口的方法---------------------------------------
    /**
     * 试图以独占模式获取。该方法应该查询对象的状态是否允许以独占模式获取它，如果允许，则获取它。
     *
     * <p>
     * 这个方法总是被执行acquire的线程调用。
     * 如果这个方法报告失败，acquire方法可能会让线程进入队列(如果它还没有进入队列)，直到它收到来自其他线程的释放信号。
     * 这可以用来实现方法{@link Lock#tryLock()}。
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
     * @return  失败值为负值;
     *          如果共享模式获取成功，但后续共享模式获取不成功，则为零;
     *          如果在共享模式中获取成功，后续的共享模式获取也可能成功，则该值为正，在这种情况下，后续的等待线程必须检查可用性。
     *          (支持三种不同的返回值使此方法可以用于有时只使用acquire的上下文。)一旦成功，这个对象就被获得了。
     *
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

    //---------------------------------------注意：final 方法---------------------------------------
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
            //释放当前节点成功
            RwAbstractQueuedSynchronizer.Node h = head;
            if (h != null && h.waitStatus != 0) {
                //唤醒节点的后继
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
     * 在共享模式下获取，如果被中断则中止。通过首先检查中断状态，然后调用至少一次{@link # tryAcquirered}，成功返回。
     * 否则，线程将被排队，可能会重复阻塞和解除阻塞，调用{@link # tryacquirered}，直到成功或线程被中断。
     *
     * @param arg 获取参数. 这个值被传递给{@link # tryacquirered}，但是它没有被解释，可以表示任何你喜欢的东西。
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0) {
            doAcquireSharedInterruptibly(arg);
        }
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

    // ----------------------队列检验方法

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
     * 查询是否有线程等待获取的时间长于当前线程。
     *
     * 调用此方法等价于(但可能更有效):
     *  <pre>
     *      {@code getFirstQueuedThread() != Thread.currentThread() && hasQueuedThreads()}
     * </pre>
     *
     * <p>注意，由于中断和超时导致的取消可能在任何时候发生，{@code true}返回不能保证其他线程会在当前线程之前获得。
     * 同样，由于队列为空，在这个方法返回{@code false}后，另一个线程也可能赢得进入队列的竞赛。
     *
     * <p>
     *     此方法被设计为公平同步器使用，以避免<a href="AbstractQueuedSynchronizer#barging">barging</a>。
     * 这样一个同步器的{@link #tryAcquire}方法应该返回{@code false}，如果这个方法返回{@code true}，
     * 那么它的{@link # tryacquiresred}方法应该返回一个负值(除非这是一个可重入的获取)。
     * 例如，一个公平、可重入、独占模式的同步器的{@code tryAcquire}方法可能是这样的:
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
     * @return 如果当前线程之前有一个正在排队的线程，则为{@code true};
     *         如果当前线程位于队列的头部或队列为空，则为{@code false}
     *
     * @since 1.7
     */
    public final boolean hasQueuedPredecessors() {
        // 这取决于head在tail和on head之前被初始化。下一个是准确的，如果当前线程在队列中是第一个。
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        RwAbstractQueuedSynchronizer.Node t = tail; // Read fields in reverse initialization order
        RwAbstractQueuedSynchronizer.Node h = head;
        RwAbstractQueuedSynchronizer.Node s;
        //为true的条件
        //1.head != tail && head.next == null
        //2.head != tail && head.next != null && head.next.thread不是当前线程
        return h != t &&
                ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // ----------------------仪器仪表和监测方法
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


    // ----------------------条件的内部支持方法

    /**
     * 如果一个节点(总是最初放置在条件队列上的节点)现在等待在同步队列上重新获取，则返回true。
     * @param node the node
     * @return true 如果是重获
     */
    final boolean isOnSyncQueue(RwAbstractQueuedSynchronizer.Node node) {
        if (node.waitStatus == RwAbstractQueuedSynchronizer.Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // 如果有后继者，它必须在队列上
            return true;
        /*
         * node.prev 可以是非空的，但不在队列上，因为将其放置在队列上的CAS可能会失败。
         * 所以我们必须从尾部横过以确保它真的成功了。
         * 在调用此方法时，它总是在尾部附近，除非CAS失败(这是不太可能的)，否则它将在那里，所以我们很少遍历。
         */
        return findNodeFromTail(node);
    }

    /**
     * 如果节点在同步队列上，则从tail往回搜索，返回true。
     * 仅当isOnSyncQueue需要时调用。
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
     * 将节点从条件队列转移到同步队列。如果成功返回true。
     *
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
         * 将线程拼接到队列上，并尝试设置前任线程的waitStatus，以指示线程(可能)正在等待。
         * 如果取消或尝试设置waitStatus失败，则唤醒并重新同步(在这种情况下，waitStatus可能是暂时的、无害的错误)。
         */
        RwAbstractQueuedSynchronizer.Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, RwAbstractQueuedSynchronizer.Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * 如果需要，在取消等待后将节点传输到同步队列
     * 如果线程在被通知之前被取消，则返回true。
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
         * 如果我们输给了一个信号()，那么我们不能继续，直到它完成它的enq()。
         * 在一个不完整的传输过程中取消是罕见的和短暂的，所以只要旋转。
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * 使用当前状态值调用释放;
     * 返回保存的状态。
     * 取消节点并在失败时抛出异常。
     *
     * @param node 这个等待的条件节点
     * @return 以前的同步状态
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
                //释放当前失败，新增节点设置为 取消
                node.waitStatus = Node.CANCELLED;
            }
        }
    }

    // ----------------------条件测量方法

    /**
     * 查询给定的条件对象是否使用此同步器作为其锁。
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
     * 作为{@link Lock}实现基础的{@link RwAbstractQueuedSynchronizer}的条件实现。
     *
     * <p>这个类的方法文档描述了机制,
     * 而不是从Lock和Condition用户的角度来看的行为规范。
     * 该类的导出版本通常需要附带描述条件语义的文档
     * ，这些条件语义依赖于关联的{@code AbstractQueuedSynchronizer}的条件语义
     *
     * <p>这个类是Serializable，但是所有字段都是瞬态的，所以反序列化的条件没有等待者。
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** 条件队列的第一个节点. */
        private transient RwAbstractQueuedSynchronizer.Node firstWaiter;
        /** 条件队列的最后一个节点. */
        private transient RwAbstractQueuedSynchronizer.Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * 添加一个新的waiter到等待队列.
         * @return its new wait node
         */
        private RwAbstractQueuedSynchronizer.Node addConditionWaiter() {
            RwAbstractQueuedSynchronizer.Node t = lastWaiter;
            // 如果尾部的等待node被取消了,则遍历取消所有的被取消的节点
            if (t != null && t.waitStatus != RwAbstractQueuedSynchronizer.Node.CONDITION) {
                // 遍历取消所有节点状态不是condition的节点
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            // 创建一个condition状态的node节点
            RwAbstractQueuedSynchronizer.Node node = new RwAbstractQueuedSynchronizer.Node(Thread.currentThread(), RwAbstractQueuedSynchronizer.Node.CONDITION);
            //如果尾结点是空证明是一个空队列,将头结点设置为当前节点,否则将当前节点插入当前尾节点的后面
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        /**
         * 删除和传输节点，直到击中未取消的节点或为空。
         * 从信号中分离出来，部分是为了鼓励编译器在没有等待者的情况下内联。
         *
         * @param first (非空)条件队列的第一个节点
         */
        private void doSignal(RwAbstractQueuedSynchronizer.Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null) {
                    lastWaiter = null;
                }
                first.nextWaiter = null;
                //将节点从条件队列转移到同步队列
            } while (!transferForSignal(first) &&
                    (first = firstWaiter) != null);
        }

        /**
         * 移除并传输所有节点。
         *
         * @param first (非空)条件队列的第一个节点
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
         * 从条件队列解除已取消的Waiter节点的链接。仅在保持锁定时调用。
         * 当在条件等待期间发生取消时，以及当lastWaiter被取消时插入一个新的Waiter时，调用该函数。
         * 这种方法是为了在没有信号的情况下避免垃圾保留。
         * 因此，即使它可能需要一个完整的遍历，它也只有在没有信号的情况下发生超时或取消时才起作用。
         * 它遍历所有节点，而不是在特定目标处停止，以断开指向垃圾节点的所有指针，而不需要在取消风暴期间多次重新遍历。
         *
         * 作用：清除队列中 waitStatus != CONDITION 的节点
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

        /** 意味着从等待退出时重新中断 */
        private static final int REINTERRUPT =  1;
        /** 意味着在退出等待时抛出InterruptedException */
        private static final int THROW_IE    = -1;

        /**
         * 等待时检查中断:
         * 如果在发出信号之前被中断返回THROW_IE，
         * 如果在发出信号之后被中断返回REINTERRUPT，
         * 如果没有被中断返回0。
         */
        private int checkInterruptWhileWaiting(RwAbstractQueuedSynchronizer.Node node) {
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }

        /**
         * 根据模式不同，抛出InterruptedException、重新中断当前线程或不执行任何操作。
         */
        private void reportInterruptAfterWait(int interruptMode)
                throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * 实现可中断条件等待.
         * <ol>
         * <li> 如果当前线程被中断，抛出InterruptedException.
         * <li> 保存由{@link #getState}返回的锁状态
         * <li> 调用{@link #release}将保存状态作为参数，如果失败抛出IllegalMonitorStateException。
         * <li> 阻塞直到收到信号或中断。
         * <li> 通过调用特殊版本的{@link #acquire}，将保存状态作为参数重新获取。
         * <li> 如果在步骤4中阻塞时被中断，抛出InterruptedException。
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            //1.将当前线程 添加到 等待队列
            RwAbstractQueuedSynchronizer.Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            //2.判断node 是否在同步队列上
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                //取消后进行清理
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * 实现定时条件等待。
         * <ol>
         * <li>如果当前线程被中断，抛出InterruptedException。
         * <li>保存{@link #getState}返回的锁状态。
         * <li>调用{@link #release}将保存状态作为参数，如果失败抛出IllegalMonitorStateException。阻塞直到收到信号、中断或超时。
         * <li>通过调用特殊版本的{@link #acquire}以保存状态作为参数重新获取。
         * <li>如果在步骤4中阻塞时被中断，抛出InterruptedException。
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
         * 实现绝对定时条件等待.
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
         * 如果该条件是由给定的同步对象创建的，则返回true。
         *
         * @return {@code true} 如果拥有
         */
        final boolean isOwnedBy(RwAbstractQueuedSynchronizer sync) {
            return sync == RwAbstractQueuedSynchronizer.this;
        }

        /**
         * 查询是否有线程在等待此条件。实现了{@link RwAbstractQueuedSynchronizer#hasWaiters(RwAbstractQueuedSynchronizer.ConditionObject)}。
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
         * 返回等待此条件的线程数的估计值。
         *
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
         *返回一个包含可能等待此条件的线程的集合。
         *
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

    // ----------------------主要CAS方法
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
