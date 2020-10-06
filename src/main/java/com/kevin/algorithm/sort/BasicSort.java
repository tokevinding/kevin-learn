package com.kevin.algorithm.sort;

import java.util.*;

/**
 * @author Kevin
 * @description 基数排序
 * 目前做的全是正数，如果出现负数，先把所有的负数分为一组（正负数分离），
 * 将负号全部去掉，然后排列，排完之后反序一次，负数的大小刚好相反。
 */
public class BasicSort {
    public void sort (int[] array){
        //获取最大值；要看排几次，主要看这个最大值有几位；
        int max = 0;
        for (int i=0;i<array.length;i++){
            if (max<array[i]){
                max = array[i];
            }
        }

        //获取最大值位数；
        int times = 0;
        while (max>0){
            max/=10;times++;//求取这个最大值的位数，依次除以10；直到为0；
        }
        List<ArrayList<Integer>> queue = new ArrayList<>();//多维数组
        for (int i=0;i<10;i++){
            ArrayList<Integer> q = new ArrayList<>();
            queue.add(q);//由于数字的特殊性，大数组中要添加10个小数组；
        }

        //开始比较,重点
        for (int i=0;i<times;i++){
            int powHighOne = (int) Math.pow(10, i + 1);
            int powBase = (int) Math.pow(10, i);

            for (int j=0;j<array.length;j++){
                //获取每次要比较的那个数字；不管是哪个位置上的；
                //获取对应位的值（i为0是个位，1是十位，2是百位）；
                int x = array[j]%powHighOne/powBase;
                ArrayList<Integer> q = queue.get(x);
                //把元素添加至对应下标数组；在小数组中添加原array的数值；
                q.add(array[j]);
                queue.set(x, q);
            }
            //开始收集；
            int count = 0;
            for (int j =0;j<10;j++){
                while (queue.get(j).size()>0){
                    //拿到每一个数组；
                    ArrayList<Integer> q = queue.get(j);
                    array[count] = q.get(0);
                    q.remove(0);
                    count++;
                }
            }
        }
    }

    public static void main(String[] args){
        BasicSort bs = new BasicSort();
        int[] a = new int[]{12,3,4,52,234,45,67,21,5,67,7};
        bs.mySort(a);
        for (int num:a){
            System.out.print(num + "|");
        }
//        System.out.println();
//        System.out.println(8%3/2);
    }


    public void mySort(int[] originArr) {
        int times = 0;
        int maxValue = originArr[0];
        //获取最大的值
        for (int i = 1; i < originArr.length; i++)
            maxValue = maxValue >= originArr[i] ? maxValue : originArr[i];
        //根据最大的值计算需要循环的次数
        for (; maxValue > 0; times++)
            maxValue/=10;

        //初始化队列集合
        List<List<Integer>> queues = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) queues.add(new ArrayList<>());

        //循环处理
        for (int i = 0; i < times; i++) {
            //获取本轮高位
            int high = (int) Math.pow(10, i+1);
            //获取本轮低位
            int low = (int) Math.pow(10, i);
            for (int j = 0; j < originArr.length; j++)
                //获取第i位数，并将其设置 当前值 所在队列集合的下标，并添加到队列
                queues.get(originArr[j] % high /low).add(originArr[j]);

            int index = 0;
            //从左到右 遍历队列集合
            for (int j = 0; j < queues.size(); j++) {

                List<Integer> levelValues = queues.get(j);
                while (levelValues.size() > 0) {
                    //
                    originArr[index++] = levelValues.get(0);
                    levelValues.remove(0);
                }
            }
        }


    }










}

