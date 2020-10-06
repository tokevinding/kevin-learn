package com.kevin.tools.utils;

import com.kevin.tools.SystemContent;

/**
 * @ClassName: BatchCountUtils
 * @Author: 丁海峰
 * @Time: 2020-05-10 10:07
 * @description:
 **/
public class BatchCountUtils {
    /**
     * 计算查询总批次数（使用默认总批次数）
     * @param totalCount 待处理总数量
     * @return 需处理总批次数
     */
    public static int count(int totalCount) {
        return (totalCount / SystemContent.DEFAULT_SINGLE_BATCH_DEAL)
                + (totalCount % SystemContent.DEFAULT_SINGLE_BATCH_DEAL == 0 ? 0 : 1);
    }
    /**
     * 计算查询总批次数
     * @param batchCount 单批次处理数量
     * @param totalCount 待处理总数量
     * @return 需处理总批次数
     */
    public static int count(int batchCount, int totalCount) {
        return (totalCount / batchCount) + (totalCount % batchCount == 0 ? 0 : 1);
    }
}
