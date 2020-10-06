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
//        printStatusValues();
//        Integer i = new Integer(-1 << 29);
//
//        System.out.println(Integer.toBinaryString(i));
//        for (int i = 0; i < 16; i++) {
//            System.out.println(Integer.toBinaryString(-i));
//        }
//        System.out.println(Integer.toBinaryString(((1 << 29) - 1)));
//        System.out.println(Integer.toBinaryString(~((1 << 29) - 1)));
//        System.out.println(tableSizeFor((1<<20) + 1));
//        System.out.println(Integer.toBinaryString((int) ((1L << 32) - 1)));
//        int bt32 = (int) ((1L << 31));
//        int max = 1 << 25;
//        for (int i = 1<< 20; i < max; i+=111) {
//            if (tableSizeFor(i) != ((i | bt32) + 1)) {
//                System.out.println(i + " | " + Integer.toBinaryString(tableSizeFor(i)) + " | " + Integer.toBinaryString((i | bt32) + 1));
//            }
//        }

        for (int i = 1 ; i < 30; i++) {
            int testN = i <= 2 ? (1 << i) : (1 << i) - 2;
            if (tableSizeFor(testN) != (1 << i)) {
                System.out.println(i+" | " + Integer.toBinaryString(testN) + " | " + Integer.toBinaryString(tableSizeFor(testN)) + " | " + Integer.toBinaryString((1 << i)));
            }
        }



/*
        LocalTime localTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        System.out.println(localTime);
        System.out.println(formatter.format(localTime));*/
    }

    /**
     * 目的：求当前数量对应的 2 ^ n (向上)
     */
    static final int tableSizeFor(int cap) {
//        System.out.println(Integer.toBinaryString(cap));
        int n = cap - 1;
//        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 1;
//        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 2;
//        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 4;
//        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 8;
//        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 16;
//        System.out.println(Integer.toBinaryString(n));
        return (n < 0) ? 1 : (n >= (1 << 30)) ? (1 << 30) : n + 1;
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
