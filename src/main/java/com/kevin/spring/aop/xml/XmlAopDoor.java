package com.kevin.spring.aop.xml;

import com.kevin.spring.aop.body.ToLogProgram;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author dinghaifeng
 * @date 2020-08-12 11:22:02
 * @desc
 */
public class XmlAopDoor {
    public static void main(String[] args) {
        ApplicationContext context =
                new ClassPathXmlApplicationContext("xml-aop-application.xml");
        ToLogProgram toLogProgram = (ToLogProgram) context.getBean("toLogProgram");
        System.out.println("===========切面方法开始执行============");
        toLogProgram.toLogMethod();
        System.out.println("===========切面方法开始执行-有返回值============");
        toLogProgram.toLogReturnMethod();
        System.out.println("===========切面方法开始执行-有抛异常============");
        toLogProgram.toLogErrorMethod();
    }
}
