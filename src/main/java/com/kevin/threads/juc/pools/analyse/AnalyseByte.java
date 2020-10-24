package com.kevin.threads.juc.pools.analyse;

import com.kevin.tools.utils.ConsoleOutputUtils;

/**
 * @author dinghaifeng
 * @date 2020-10-10 13:54:13
 * @desc ThreadPoolExecutor 位运算分析
 */
public class AnalyseByte extends BaseAnalyse {

    public static void main(String[] args) {
        printStatusValues();
    }

    protected static int[] statuses = new int[]{RUNNING, SHUTDOWN, STOP, TIDYING, TERMINATED};

    static void printStatusValues() {
        for (int i = 0; i < statuses.length; i++) {
            int status = statuses[i];
            int ctlOf = ctlOf(status, 0);
            System.out.println("status binary | " + Integer.toBinaryString(status));
            System.out.println("ctlOf(status) | " + Integer.toBinaryString(ctlOf));
            System.out.println("workerCountOf(ctlOf(status)) | " + Integer.toBinaryString(workerCountOf(ctlOf)));
            System.out.println("runStateOf(ctlOf(status)) | " + Integer.toBinaryString(runStateOf(ctlOf)));
            ConsoleOutputUtils.hr();
        }
    }
}
