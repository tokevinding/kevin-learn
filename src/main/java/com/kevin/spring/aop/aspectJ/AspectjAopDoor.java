package com.kevin.spring.aop.aspectJ;

import com.kevin.spring.aop.body.ToLogProgram;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Kevin
 * @date 2020-08-12 11:22:02
 * @desc
 */
public class AspectjAopDoor {
    public static void main(String[] args) {
        ApplicationContext context =
                new ClassPathXmlApplicationContext("aspectj-aop-application.xml");
        ToLogProgram toLogProgram = (ToLogProgram) context.getBean("toLogProgram");
        System.out.println("===========切面方法开始执行============");
        toLogProgram.toLogMethod();
        System.out.println("===========切面方法开始执行-有返回值============");
        toLogProgram.toLogReturnMethod();
        System.out.println("===========切面方法开始执行-有抛异常============");
        toLogProgram.toLogErrorMethod();
    }
}
