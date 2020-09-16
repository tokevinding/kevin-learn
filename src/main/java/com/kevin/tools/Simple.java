package com.kevin.tools;

import com.kevin.tools.utils.ConsoleOutputUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dinghaifeng
 * @date 2020-09-14 18:52:16
 * @desc
 */
public class Simple {
    public static void main(String[] args) {
        printStatusValues();
//        Integer i = new Integer(-1 << 29);
//
//        System.out.println(Integer.toBinaryString(i));
        for (int i = 0; i < 16; i++) {
            System.out.println(Integer.toBinaryString(-i));
        }

    }

    void testBitCode() {
        String a = "a";
        String b = "b";
        String c = "c";
        String d = a + b + c + "c";
    }

    static void printStatusValues() {
        for (int i = 0; i < statuses.length; i++) {
            int status = statuses[i];
            int ctlOf = ctlOf(status, 0);
            System.out.println("status binary | " + Integer.toBinaryString(status));
            System.out.println("ctlOf(status) | " + ctlOf);
            System.out.println("workerCountOf(ctlOf(status)) | " + workerCountOf(ctlOf));
            System.out.println("runStateOf(ctlOf(status)) | " + runStateOf(ctlOf));
            ConsoleOutputUtils.hr();
        }
    }

    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    private static final int RUNNING    = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;

    private static int[] statuses = new int[]{RUNNING, SHUTDOWN, STOP, TIDYING, TERMINATED};

    // Packing and unpacking ctl
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    private static int ctlOf(int rs, int wc) { return rs | wc; }
}
