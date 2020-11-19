package com.kevin.threads.juc.pools.analyse;

import com.kevin.tools.utils.ConsoleOutputUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kevin
 * @date 2020-10-16 17:32:45
 * @desc
 */
public class BaseAnalyse {

    protected final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    protected static final int COUNT_BITS = Integer.SIZE - 3;
    /**
     * 最大容量: 000 11111111111111111111111111111
     */
    protected static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits

    //如下状态常量（只有状态位）
    /**
     * 运行中状态
     * 111 00000000000000000000000000000
     */
    protected static final int RUNNING    = -1 << COUNT_BITS;
    /**
     * 000 00000000000000000000000000000
     */
    protected static final int SHUTDOWN   =  0 << COUNT_BITS;
    /**
     * 001 00000000000000000000000000000
     */
    protected static final int STOP       =  1 << COUNT_BITS;
    /**
     * 010 00000000000000000000000000000
     */
    protected static final int TIDYING    =  2 << COUNT_BITS;
    /**
     * 011 00000000000000000000000000000
     */
    protected static final int TERMINATED =  3 << COUNT_BITS;

    // Packing and unpacking ctl
    /**
     * 获取当前运行状态
     * ~CAPACITY ：111 00000000000000000000000000000
     * 取与，数量位全为0，状态位依据 c 确定
     */
    protected static int runStateOf(int c)     { return c & ~CAPACITY; }

    /**
     * CAPACITY ：000 11111111111111111111111111111
     * 取与，状态位全为0，数量位依据 c 确定
     */
    protected static int workerCountOf(int c)  { return c & CAPACITY; }

    /**
     * 合并 状态位 和 数量位
     * @param rs 状态位 xxx 00000000000000000000000000000
     * @param wc 数量位 000 xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     * @return 合并后的结果
     */
    protected static int ctlOf(int rs, int wc) { return rs | wc; }
}
