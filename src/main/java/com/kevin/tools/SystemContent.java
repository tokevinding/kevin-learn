package com.kevin.tools;

/**
 * program: kevinutils
 * author: kevin
 * create time: 2019-08-22 15:24
 * description:
 **/
public class SystemContent {

    /**
     * 获取跟编译结果根目录的路径
     * @return
     */
    public static String getRootPath() {
        return SystemContent.class.getResource("/").getPath();
    }

    /**
     * 是否统一单位：
     *      true：时间单位转化为-小时，价格单位转化为-元
     */
    public static final boolean IS_UNIFIED = true;

    /**
     * 参数补位符
     */
    public static final String PARAM_FILL_FLAG = "%s";

    /**
     * 默认时间
     */
    public static final String DEFAULT_SYSTEM_STRING = "1000-01-01 00:00:00";

    /**
     * 每月第一天
     */
    public static final Integer FIRST_DAY_OF_MONTH = 1;
    /**
     * 默认文件后缀
     */
    public static final String DEFAULT_FILE_SUFFIX = ".txt";
    /**
     * 默认文件基本路径
     */
    public static final String DEFAULT_BASE_PATH = "/Users/er/Documents/dev/data/general/";

    /**
     * 系统默认单批次处理数量
     */
    public static final int DEFAULT_SINGLE_BATCH_DEAL = 500;
}
