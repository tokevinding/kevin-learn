package com.kevin.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

/**
 * @author Kevin
 * @date 2020-08-14 11:05:03
 * @desc
 */
@SpringBootApplication(scanBasePackages = {"com.kevin.spring"})
public class SpringApplicationRun {
    public static void main(String[] args) {
        SpringApplication.run(SpringApplicationRun.class, args);
    }
}
