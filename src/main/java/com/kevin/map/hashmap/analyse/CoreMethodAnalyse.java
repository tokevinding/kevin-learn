package com.kevin.map.hashmap.analyse;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author dinghaifeng
 * @date 2020-11-19 13:54:11
 * @desc HashMap核心方法分析
 */
public class CoreMethodAnalyse extends CoreParamAnalyse {

    public static void main(String[] args) {
        String s19 = "1011000100010001000";
        int i = Integer.parseInt(s19, 2);
        System.out.println(Integer.toBinaryString(i));
        System.out.println(Integer.toBinaryString(hash(i)));
        System.out.println();
        System.out.println(Integer.toBinaryString((11 ^ 1)));
    }

    /**
     * 官方说明：
     * 计算key.hashCode()并将哈希值的较高位扩展到较低位。
     * 因为表使用了2次幂屏蔽，所以只在当前掩码上方变化的散列集总是会发生冲突。
     * (已知的例子包括在小表中保存连续整数的浮点键集。)所以我们应用一种变换，将更高位的影响向下传播。
     * 在速度、效用和比特传播的质量之间需要权衡。因为许多常见的散列集已经合理分布(所以不要受益于传播)
     * ,因为我们用树来处理大型的碰撞在垃圾箱,我们只是XOR一些改变以最便宜的方式来减少系统lossage
     * ,以及将最高位的影响,否则永远不会因为指数计算中使用的表。
     *
     * 个人理解：
     * 计算方式：高16位不变，低16位取（高16位 与 低16位）异或运算(不同为1，同为0) 的结果
     * 目的: 降低哈希碰撞
     *
     * 测试代码：
     * h = key.hashCode();
     * System.out.println();
     * System.out.println(Integer.toBinaryString((h)));
     * System.out.println(Integer.toBinaryString((h >>> 16)));
     * System.out.println(Integer.toBinaryString(h ^ (h >>> 16)));
     * System.out.println();
     */
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }



    /**
     * 返回给定目标容量的2倍幂
     * 如原始值:
     *  值：       17
     *  二进制：    00000000 00000000 00000000 00010001
     *
     * 目的需要把参数转为：
     *  值：       32（2^5）
     *  二进制：    00000000 00000000 00000000 00100000
     */
    static final int tableSizeFor(int cap) {
        //减1，是为了处理当前值为 2的n次幂 的情况
        int n = cap - 1;
        //无符号右移，即使位数不够也不会对前面的（已补1的位）产生影响
        //首1 后1位补 1
        n |= n >>> 1;
        //首1 (后1位已补)后3位补 1
        n |= n >>> 2;
        //首1 (后3位已补)后7位补 1
        n |= n >>> 4;
        //首1 (后7位已补)后15位补 1
        n |= n >>> 8;
        //首1 (后15位已补)后31位补 1
        n |= n >>> 16;
        //如果 不大于/等于 最大容量，需要 +1 处理，获取2的n次幂
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }


}
