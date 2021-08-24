package com.kevin.threads.juc.aqs.sub.blockQueue;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * 这个类提供了一些{@link Queue}操作的骨架实现。
 * 当基本实现<em>not</em> allow <tt>null</tt> elements时，这个类中的实现是合适的。
 * 方法{@link #add add}、{@link #remove remove}和{@link #element element}
 * 分别基于{@link #offer offer}、{@link #poll poll}和{@link #peek peek}，
 * 但抛出异常而不是通过<tt>false</tt>或<tt>null</tt>返回。
 *
 * 扩展这个类的一个<tt>Queue</tt>实现必须最少定义一个不允许插入<tt>null</tt>元素的方法{@link Queue#offer}，
 * 以及方法{@link Queue#peek}， {@link Queue#poll}， {@link Collection#size}
 * 和{@link Collection#iterator}。
 * 通常，其他方法也会被覆盖。
 * 如果不能满足这些要求，可以考虑子类化{@link AbstractCollection}。
 *
 * 这个类是<a href="{@docRoot}/../technotes/guides/collections/index.html"的成员
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public abstract class RwAbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {

    /**
     * Constructor for use by subclasses.
     */
    protected RwAbstractQueue() {
    }

    /**
     * 如果可以立即将指定的元素插入到该队列中而不违反容量限制，则在成功时返回<tt>true</tt>，
     * 如果当前没有可用空间，则抛出<tt>IllegalStateException</tt>。
     *
     * <p>This implementation returns <tt>true</tt> if <tt>offer</tt> succeeds,
     * else throws an <tt>IllegalStateException</tt>.
     *
     * @param e the element to add
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     *         this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *         prevents it from being added to this queue
     */
    @Override
    public boolean add(E e) {
        if (offer(e)) {
            return true;
        } else {
            throw new IllegalStateException("Queue full");
        }
    }

    /**
     * 检索并删除此队列的头部。此方法与{@link #poll poll}的不同之处在于，如果队列为空，则抛出异常。
     *
     * <p>This implementation returns the result of <tt>poll</tt>
     * unless the queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public E remove() {
        E x = poll();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * 检索但不删除此队列的头部。此方法与{@link #peek}的不同之处在于，如果队列为空，则抛出异常。
     *
     * <p>这个实现返回<tt>peek</tt>的结果，除非队列是空的。
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public E element() {
        E x = peek();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * 从这个队列中删除所有元素。此调用返回后，队列将为空。
     *
     * <p>This implementation repeatedly invokes {@link #poll poll} until it
     * returns <tt>null</tt>.
     */
    @Override
    public void clear() {
        while (poll() != null) {
        }
    }

    /**
     * Adds all of the elements in the specified collection to this
     * queue.  Attempts to addAll of a queue to itself result in
     * <tt>IllegalArgumentException</tt>. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * <p>This implementation iterates over the specified collection,
     * and adds each element returned by the iterator to this
     * queue, in turn.  A runtime exception encountered while
     * trying to add an element (including, in particular, a
     * <tt>null</tt> element) may result in only some of the elements
     * having been successfully added when the associated exception is
     * thrown.
     *
     * @param c collection containing elements to be added to this queue
     * @return <tt>true</tt> if this queue changed as a result of the call
     * @throws ClassCastException if the class of an element of the specified
     *         collection prevents it from being added to this queue
     * @throws NullPointerException if the specified collection contains a
     *         null element and this queue does not permit null elements,
     *         or if the specified collection is null
     * @throws IllegalArgumentException if some property of an element of the
     *         specified collection prevents it from being added to this
     *         queue, or if the specified collection is this queue
     * @throws IllegalStateException if not all the elements can be added at
     *         this time due to insertion restrictions
     * @see #add(Object)
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

}
