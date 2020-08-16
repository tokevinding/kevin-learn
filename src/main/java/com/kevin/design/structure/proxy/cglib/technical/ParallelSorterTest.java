package com.kevin.design.structure.proxy.cglib.technical;

import net.sf.cglib.util.ParallelSorter;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class ParallelSorterTest {
    public static void main(String[] args) {
        Integer[][] value = {
                {4, 3, 9, 0},
                {2, 1, 6, 0}
        };
        ParallelSorter.create(value).mergeSort(0);
        for(Integer[] row : value){
            int former = row[0] - 1;
            for(int val : row) {
                if (former > val) {
                    System.out.println("排序异常");
                }
                former = val;
            }
        }
    }
}