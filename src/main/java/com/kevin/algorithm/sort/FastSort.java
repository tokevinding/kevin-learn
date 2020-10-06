package com.kevin.algorithm.sort;

/**
 * @author dinghaifeng
 * @description 快速排序
 * 思想：挖坑，填坑（通常内循环，左挖坑，先从右向左查（大/小于flag），后从左向右查（小/大于flag））
 * 重要的是边界值要控制好
 */
public class FastSort {
    public void sort (int[] array, int start, int end) {
        if (start >= end) {
            return;
        }
        int middle = array[start];
        int s = start, e = end;
        while (s < e) {
            while (s < e && array[e] >= middle) {
                e--;
            }
            if (s < e) {
                array[s] = array[e];
                s++;
            }
            while (s < e && array[s] < middle) {
                s++;
            }
            if (s < e) {
                array[e] = array[s];
                e--;
            }
        }
        array[s] = middle;
        sort(array, start, s - 1);
        sort(array, e + 1, end);
    }

    public static void main(String[] args){
        FastSort sort = new FastSort();
        int[] a = new int[]{12,3,4,52,234,45,67,21,5,67,7};
        sort.sort(a, 0, a.length - 1);
        for (int num : a){
            System.out.print(num + "|");
        }
    }

}

