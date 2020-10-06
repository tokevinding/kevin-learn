package com.kevin.algorithm.sort;

/**
 * @author Kevin
 * @description 插入排序
 *
 */
public class InsertSort {

    public void sort (int[] array) {
        for (int i = 1; i < array.length; i++) {
            int hole = array[i];
            int cursor = i - 1;
            while (cursor >= 0 && array[cursor] > hole) {
                array[cursor + 1] = array[cursor];
                cursor--;
            }
            array[cursor + 1] = hole;
        }
    }

    public static void main(String[] args){
        InsertSort sort = new InsertSort();
        int[] a = new int[]{12,3,4,52,234,45,67,21,5,67,7};
        sort.sort(a);
        for (int num : a) {
            System.out.print(num + "|");
        }
    }

}

