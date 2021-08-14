package com.kevin.threads.juc.aqs.use;


import com.kevin.threads.juc.aqs.rw.RwAbstractQueuedSynchronizer;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 计数信号量。从概念上讲，信号量维护一组许可。每个{@link #acquire}如果需要的话会阻塞，直到一个许可可用，然后获取它。
 * 每个{@link #release}添加一个permit，可能释放一个阻塞获取器。
 * 但是，没有使用实际的permit对象;{@code Semaphore}只保留一个可用的数，并相应地进行操作。
 *
 * <p>Semaphores are often used to restrict the number of threads than can
 * access some (physical or logical) resource. For example, here is
 * a class that uses a semaphore to control access to a pool of items:
 *  <pre> {@code
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          } else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 * }}</pre>
 *
 * <p>Before obtaining an item each thread must acquire a permit from
 * the semaphore, guaranteeing that an item is available for use. When
 * the thread has finished with the item it is returned back to the
 * pool and a permit is returned to the semaphore, allowing another
 * thread to acquire that item.  Note that no synchronization lock is
 * held when {@link #acquire} is called as that would prevent an item
 * from being returned to the pool.  The semaphore encapsulates the
 * synchronization needed to restrict access to the pool, separately
 * from any synchronization needed to maintain the consistency of the
 * pool itself.
 *
 * <p>A semaphore initialized to one, and which is used such that it
 * only has at most one permit available, can serve as a mutual
 * exclusion lock.  This is more commonly known as a <em>binary
 * semaphore</em>, because it only has two states: one permit
 * available, or zero permits available.  When used in this way, the
 * binary semaphore has the property (unlike many {@link java.util.concurrent.locks.Lock}
 * implementations), that the &quot;lock&quot; can be released by a
 * thread other than the owner (as semaphores have no notion of
 * ownership).  This can be useful in some specialized contexts, such
 * as deadlock recovery.
 *
 * <p> The constructor for this class optionally accepts a
 * <em>fairness</em> parameter. When set false, this class makes no
 * guarantees about the order in which threads acquire permits. In
 * particular, <em>barging</em> is permitted, that is, a thread
 * invoking {@link #acquire} can be allocated a permit ahead of a
 * thread that has been waiting - logically the new thread places itself at
 * the head of the queue of waiting threads. When fairness is set true, the
 * semaphore guarantees that threads invoking any of the {@link
 * #acquire() acquire} methods are selected to obtain permits in the order in
 * which their invocation of those methods was processed
 * (first-in-first-out; FIFO). Note that FIFO ordering necessarily
 * applies to specific internal points of execution within these
 * methods.  So, it is possible for one thread to invoke
 * {@code acquire} before another, but reach the ordering point after
 * the other, and similarly upon return from the method.
 * Also note that the untimed {@link #tryAcquire() tryAcquire} methods do not
 * honor the fairness setting, but will take any permits that are
 * available.
 *
 * <p>Generally, semaphores used to control resource access should be
 * initialized as fair, to ensure that no thread is starved out from
 * accessing a resource. When using semaphores for other kinds of
 * synchronization control, the throughput advantages of non-fair
 * ordering often outweigh fairness considerations.
 *
 * <p>This class also provides convenience methods to {@link
 * #acquire(int) acquire} and {@link #release(int) release} multiple
 * permits at a time.  Beware of the increased risk of indefinite
 * postponement when these methods are used without fairness set true.
 *
 * <p>Memory consistency effects: Actions in a thread prior to calling
 * a "release" method such as {@code release()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions following a successful "acquire" method such as {@code acquire()}
 * in another thread.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class RwSemaphore implements java.io.Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    /** All mechanics via AbstractQueuedSynchronizer subclass */
    private final Sync sync;

    /**
     * 信号量的同步实现。使用AQS状态表示许可。派生为公平和非公平版本。
     */
    abstract static class Sync extends RwAbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
            setState(permits);
        }

        final int getPermits() {
            return getState();
        }

        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                        compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }

        @Override
        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (next < current) // overflow
                {
                    throw new Error("Maximum permit count exceeded");
                }
                if (compareAndSetState(current, next)) {
                    return true;
                }
            }
        }

        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                int next = current - reductions;
                if (next > current) // underflow
                {
                    throw new Error("Permit count underflow");
                }
                if (compareAndSetState(current, next)) {
                    return;
                }
            }
        }

        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0)) {
                    return current;
                }
            }
        }
    }

    /**
     * NonFair version
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        @Override
        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * Fair version
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        @Override
        protected int tryAcquireShared(int acquires) {
            for (;;) {
                if (hasQueuedPredecessors()) {
                    return -1;
                }
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                        compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }
    }

    /**
     * 创建一个具有给定数量的许可和非公平公平设置的{信号量}。
     *
     * @param permits 可用许可的初始数量。这个值可能是负的，在这种情况下，必须在授予任何获取之前进行释放。
     */
    public RwSemaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * 创建一个{@code 信号量}，具有给定的许可数量和给定的公平性设置。
     *
     * @param permits 可用许可的初始数量。这个值可能是负的，在这种情况下，必须在授予任何获取之前进行释放。
     * @param fair {@code true} 如果这个信号量保证在争用的情况下先入先出授予许可，则{@code false}
     */
    public RwSemaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * 从这个信号量获得许可, 阻塞直到一个可用, 或者线程被中断
     *
     * <p> 获得许可证(如果有的话)，然后立即返回，使可用许可证的数量减少一个。
     *
     * 如果没有permit可用，那么当前线程将被禁用用于线程调度，并处于休眠状态，直到发生以下两种情况之一:
     * 1.其他线程调用这个信号量的{@link #release}方法，当前线程是下一个被分配许可的线程;
     * 2.其他线程中断了当前线程
     *
     * 如果当前线程符合如下条件:
     * 1.是否在进入该方法时设置了中断状态
     * 2.在等待许可证的时候被中断
     * 处理：抛出{@link InterruptedException}，清除当前线程的中断状态
     *
     * @throws InterruptedException 如果当前线程被中断
     */
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * 从这个信号量获得许可，阻塞直到一个可用
     *
     * 获得一个许可(如果一个是可用的)，并立即返回，将可用许可的数量减少一个。
     *
     * 如果没有可用的许可，那么当前线程将被禁用用于线程调度，并处于休眠状态
     * 直到其他线程调用这个信号量的{@link #release}方法，并且当前线程接下来将被分配一个许可。
     *
     * 如果当前线程在等待许可的时候被中断 然后它将继续等待，但是与没有中断时线程收到许可的时间相比
     * ，线程获得许可的时间可能会发生变化。当线程确实从这个方法返回时，它的中断状态将被设置。
     */
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * 仅当调用时一个信号量可用时，才从这个信号量获得许可。
     *
     * 获取一个许可(如果有一个可用且立即返回)，值为{@code true}，将可用许可的数量减少一个。
     * 如果没有许可可用，那么该方法将立即返回值{@code false}。
     * 即使这个信号量被设置为使用公平排序策略，调用{@code tryAcquire()} <em>将</em>立即获得一个许可，如果一个许可是可用的，不管其他线程是否正在等待。
     *
     * 这个冲动的行为在某些情况下是有用的，即使它违反了公平。
     * 如果你想遵守公平设置，那么使用{@link #tryAcquire(long, TimeUnit) tryAcquire(0, TimeUnit. seconds)}，这几乎是等效的(它也检测中断)。
     *
     * @return {@code true} 如果获得了许可证 and {@code false} otherwise
     */
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * 如果一个信号量在给定的等待时间内可用，并且当前线程还没有被中断，则从这个信号量获得一个许可
     * 获取一个许可(如果有一个可用且立即返回)，值为{@code true}，将可用许可的数量减少一个。
     *
     * 如果没有permit可用，那么当前线程将*为线程调度目的而被禁用，并且处于休眠状态，直到发生以下三种情况之一:
     * 1.其他线程调用这个信号量的{@link #release}方法，当前线程是下一个被分配许可的线程;
     * 2.其他线程中断了当前线程
     * 3.等待时间过了
     *
     * 如果获得了许可，则返回值{@code true}。
     *
     * 如果当前线程存在如下情况:
     * 1.是否在进入该方法时设置了中断状态
     * 2.在等待许可证的时候被中断
     * 处理：抛出{@link InterruptedException}，清除当前线程的中断状态
     *
     * 如果指定的等待时间过了，则返回值{@code false}。如果时间小于或等于零，则该方法根本不会等待。
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     *         if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 释放一个许可，将它返回给信号量。
     *
     * 释放一个许可，将可用的许可数量增加一个。如果任何线程试图获得许可，那么将选择一个线程并给予刚刚释放的许可。该线程被(重新)启用以进行线程调度。
     *
     * 没有要求释放许可的线程必须通过调用{@link #acquire}获得该许可。信号量的正确使用是通过应用程序中的编程约定来确定的。
     */
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * 从这个信号量获取给定数量的许可，阻塞直到所有的许可都可用，或者当前线程被中断
     * 获得给定数量的许可(如果它们是可用的)，并立即返回，将可用许可的数量减少给定数量。
     *
     * 如果没有足够的许可可用，那么当前线程将被禁用以进行线程调度，并且处于休眠状态，直到发生以下两种情况之一:
     * 1.其他线程调用这个信号量的{@link #release() release}方法，当前线程是下一个被分配许可的线程，可用许可的数量满足这个请求
     * 2.在等待许可证的时候被中断
     *
     * 如果当前线程存在如下情况:
     * 1.在进入该方法时设置中断状态;
     * 2.在等待许可证的时候被中断
     * 处理：抛出{@link InterruptedException}，清除当前线程的中断状态
     * 并且 任何要分配给这个线程的许可都会被分配给其他试图获取许可的线程，就好像通过调用{@link #release()}可以获得许可一样。
     *
     * @param permits the number of permits to acquire
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        sync.acquireSharedInterruptibly(permits);
    }

    /**
     * 从这个信号量获取给定数量的许可，阻塞直到所有许可都可用。
     * 获得给定数量的许可(如果它们是可用的)，并立即返回，将可用许可的数量减少给定数量。
     *
     * 如果没有足够的可用许可证那么当前线程成为残疾人用于线程调度和谎言休眠
     * ,直到其他线程调用一个{@link #release()}释放这个信号量的方法,当前线程几乎是被分配许可和可用许可证的数量满足这个要求。
     *
     * 在等待许可证的时候被中断 它将继续等待，并且它在队列中的位置不受影响。当线程确实从这个方法返回时，它的中断状态将被设置。
     *
     * @param permits the number of permits to acquire
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        sync.acquireShared(permits);
    }

    /**
     * 从这个信号量中获取给定的许可数量，只有在调用时所有许可都可用时才使用
     * 获取给定数量的许可(如果它们是可用的)，并立即返回，值为{@code true}，将可用许可的数量减少到给定数量。
     *
     * 如果可用许可不足，则此方法将立即返回值{@code false}，且可用许可的数量不变
     *
     * 即使这个信号量被设置为使用公平排序策略，调用{@code tryAcquire} <em>将</em>立即获得一个许可，如果一个许可是可用的
     * ，不管其他线程是否正在等待。这个“barging&quot;行为在某些情况下是有用的，即使它违反了公平。
     * 如果你想遵守公平性设置，那么使用{@link #tryAcquire(int, long, TimeUnit) tryAcquire(permit, 0, TimeUnit. seconds)}，这几乎是等效的(它也检测中断)。
     *
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired and
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    /**
     * 如果在给定的等待时间内所有的许可都可用，并且当前线程还没有被中断，则从这个信号量中获取给定的许可数量。
     * 获取给定数量的许可，如果它们是可用的，并立即返回，值为{@code true}，将可用许可的数量减少给定数量。
     *
     * 如果没有足够的许可可用，那么当前线程将被禁用以进行线程调度，并且处于休眠状态，直到发生以下三种情况之一:
     * 1.其他线程调用这个信号量的{@link #release() release}方法，当前线程是下一个被分配许可的线程，可用许可的数量满足这个请求
     * 2.在等待许可证的时候被中断
     * 3.等待时间过了
     * 如果获得了许可，则返回值{@code true}。
     *
     * 如果当前线程:
     * 1.在进入该方法时设置中断状态;
     * 2.在等待许可证的时候被中断
     * 处理：抛出{@link InterruptedException}，清除当前线程的中断状态
     * 并且 任何要分配给这个线程的许可都会被分配给其他试图获取许可的线程，就好像通过调用{@link #release()}可以获得许可一样。
     *
     * 如果指定的等待时间过了，则返回值{@code false}。如果时间小于或等于零，则该方法根本不会等待。
     * 任何被分配给这个线程的许可，都会被分配给其他试图获得许可的线程，就像通过调用{@link #release()}来获得许可一样。
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if all permits were acquired and {@code false}
     *         if the waiting time elapsed before all permits were acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    /**
     * 释放给定数量的许可，将它们返回给信号量。
     *
     * 释放给定的许可数量，将可用的许可数量增加相应数量。如果任何线程试图获得许可，那么将选择一个线程并给予刚刚释放的许可。
     * 如果可用许可的数量满足该线程的请求，则该线程被(重新)启用用于线程调度目的;否则线程将等待，直到有足够的许可可用。
     * 如果在此线程的请求得到满足后仍然有可用的许可，那么这些许可将依次分配给试图获得许可的其他线程。
     *
     * 没有要求释放许可的线程必须通过调用{@link java.util.concurrent.Semaphore#acquire acquire}获得。
     * 信号量的正确使用是通过应用程序中的编程约定来确定的。
     *
     * @param permits the number of permits to release
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void release(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException();
        }
        sync.releaseShared(permits);
    }

    /**
     * 返回此信号量中当前可用的许可数。
     *
     * 该方法通常用于调试和测试目的。
     *
     * @return 这个信号量中可用的允许数
     */
    public int availablePermits() {
        return sync.getPermits();
    }

    /**
     * 获得并退还所有可立即获得的许可证。
     *
     * @return 获得的许可证数量
     */
    public int drainPermits() {
        return sync.drainPermits();
    }

    /**
     * 通过指示的减少减少可用许可的数量。这个方法在使用信号量跟踪不可用资源的子类中很有用。
     * 这个方法不同于{@code acquire}，因为它不会阻塞等待许可可用。
     *
     * @param reduction the number of permits to remove
     * @throws IllegalArgumentException 如果{@code reduction}为负数
     */
    protected void reducePermits(int reduction) {
        if (reduction < 0) {
            throw new IllegalArgumentException();
        }
        sync.reducePermits(reduction);
    }

    /**
     * Returns {@code true} if this semaphore has fairness set true.
     *
     * @return {@code true} if this semaphore has fairness set true
     */
    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 查询是否有线程正在等待获取。注意，因为取消可能在任何时候发生，
     * {@code true}返回并不保证任何其他线程将获得。这种方法主要是为监控系统状态而设计的。
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire.
     * The value is only an estimate because the number of threads may
     * change dynamically while this method traverses internal data
     * structures.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to acquire.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a best-effort
     * estimate.  The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * Returns a string identifying this semaphore, as well as its state.
     * The state, in brackets, includes the String {@code "Permits ="}
     * followed by the number of permits.
     *
     * @return a string identifying this semaphore, as well as its state
     */
    @Override
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}
