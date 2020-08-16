package com.kevin.spring.aop.body;

import org.springframework.stereotype.Component;

/**
 * @author dinghaifeng
 * @date 2020-08-12 11:18:09
 * @desc
 */
@Component
public class ToLogProgram {

    public void toLogMethod() {
        System.out.println("++++++方法体++++++");
    }

    public String toLogReturnMethod() {
        System.out.println("++++++方法体++++++");
        return "success";
    }

    public void toLogErrorMethod() {
        System.out.println("++++++方法体++++++");
        throw new RuntimeException("方法执行异常！！");
    }
}
