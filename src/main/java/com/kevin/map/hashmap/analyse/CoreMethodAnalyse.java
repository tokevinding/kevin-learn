package com.kevin.map.hashmap.analyse;

/**
 * @author dinghaifeng
 * @date 2020-11-19 13:54:11
 * @desc HashMap核心参数分析
 */
public class CoreMethodAnalyse {

    /**
     * 默认初始容量-必须是2的幂。(16)
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

    /**
     * 最大容量，如果任何一个带参数的构造函数隐式指定了较大的值，则使用。必须是2的幂 <= 1<<30。
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 在构造函数中未指定时使用的加载因子。
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 为容器使用树而不是列表时的容器计数阈值。
     * 1.当向至少有这么多节点的bin中添加一个元素时，bin被转换为树。
     * 2.该值必须大于2，并且至少应该是8，以符合在树移除时关于在收缩时转换回普通箱的假设。
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 在调整大小操作期间对(分割)bin进行重新格式化的bin计数阈值。
     * 1.应小于TREEIFY_THRESHOLD，
     * 2.最多6与收缩检测下去除。
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 容器可能被treeified的最小表容量。(否则，如果容器中有太多节点，就会调整表的大小。)
     * 1.应该至少 4 * TREEIFY_THRESHOLD，以避免大小调整和treeification阈值之间的冲突。
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

}
