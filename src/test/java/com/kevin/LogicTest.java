package com.kevin;

import com.google.common.collect.Lists;
import com.kevin.common.utils.json.JsonUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dinghaifeng
 * @date 2020-11-19 14:59:13
 * @desc
 */
public class LogicTest {
    public static void main(String[] args) {
        List<Integer> originList = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
        int totalCount = originList.size();
        int batchDealCount = 4;

        int iteratorCount = (totalCount / batchDealCount) + (totalCount % batchDealCount == 0 ? 0 : 1);
        for (int i = 0; i < iteratorCount; i++) {
            List<Integer> subOrderIds = originList.stream().skip(i * batchDealCount)
                    .limit(batchDealCount).collect(Collectors.toList());
            System.out.println(JsonUtil.toString(subOrderIds));
        }
    }
}
