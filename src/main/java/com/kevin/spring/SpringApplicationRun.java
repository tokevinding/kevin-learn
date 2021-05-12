package com.kevin.spring;

import com.kevin.spring.composite.CompositeBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.Resource;

/**
 * @author Kevin
 * @date 2020-08-14 11:05:03
 * @desc
 */
@SpringBootApplication(scanBasePackages = {"com.kevin.spring.composite"})
public class SpringApplicationRun {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpringApplicationRun.class, args);
        CompositeBean bean = context.getBean(CompositeBean.class);
        System.out.println(bean);
    }
}
