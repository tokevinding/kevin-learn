package com.kevin.spring.aop.boot;

import com.kevin.spring.aop.body.ToLogProgram;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author dinghaifeng
 * @date 2020-08-12 11:14:57
 * @desc
 */
@Component
public class AopMethodCaller {
    @Resource
    private ToLogProgram toLogProgram;

    @PostConstruct
    public void initAfterDo() {
        toLogProgram.toLogMethod();
        toLogProgram.toLogReturnMethod();
        toLogProgram.toLogErrorMethod();
    }

}
