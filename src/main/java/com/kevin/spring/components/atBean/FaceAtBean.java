package com.kevin.spring.components.atBean;

import com.kevin.spring.components.CountThreadLocal;
import com.kevin.spring.components.helpBean.KevinAtImportClass;
import com.kevin.spring.components.helpBean.KevinPostClass;
import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Component
public class FaceAtBean {
    @Bean
    public KevinPostClass kevinPostClass() {
        ConsoleOutputUtils.hr(CountThreadLocal.incrementAndGet() + " FaceAtBean.kevinPostClass");
        return new KevinPostClass();
    }
}
