package com.kevin.threads.juc.aqs.use;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author dinghaifeng
 * @date 2021-04-25 18:28:31
 * @desc
 */
public class UseLinkedTransferQueue {

    public static void main(String[] args) {
        final LinkedTransferQueue<Integer> transferQueue = new LinkedTransferQueue<>();
        UseUtils.set(e -> {
            try {
                transferQueue.transfer(e);
                UseUtils.printThread("transfer", true, e);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                UseUtils.printThread("transfer ex", false, e);
            }
        });

//        UseUtils.getMultiThread(() -> {
        UseUtils.getMultiThreadCurrentStart(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
                Integer take = transferQueue.take();
                UseUtils.printThread("take", true, take);
                return take;
            } catch (InterruptedException ex) {
                UseUtils.printThread("take", false, "", "异常");
            }
            return null;
        });
    }

}
