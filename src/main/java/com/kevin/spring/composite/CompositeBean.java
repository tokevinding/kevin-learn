package com.kevin.spring.composite;

import com.kevin.spring.components.CountThreadLocal;
import com.kevin.spring.components.helpBean.KevinAtImportClass;
import com.kevin.spring.components.helpBean.KevinPostClass;
import com.kevin.spring.components.importBean.FaceImportBeanDefinitionRegistrar;
import com.kevin.spring.components.importBean.FaceImportSelector;
import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author dinghaifeng
 * @date 2021-04-28 13:49:51
 * @desc 综合型测试
 */
@Component
@Import({KevinAtImportClass.class, FaceImportBeanDefinitionRegistrar.class, FaceImportSelector.class})
public class CompositeBean  implements ImportSelector
        , BeanFactoryAware, BeanNameAware, ApplicationContextAware, EnvironmentAware, BeanClassLoaderAware
        , InitializingBean, DisposableBean
        , BeanDefinitionRegistryPostProcessor, BeanPostProcessor {

    @Bean
    public KevinPostClass kevinPostClass() {
        ConsoleOutputUtils.hrLineNumber("@Bean");
        return new KevinPostClass();
    }

    @PostConstruct
    public void postConstruct() {
        ConsoleOutputUtils.hrLineNumber( "@PostConstruct");
        ConsoleOutputUtils.hr( "@PostConstruct");
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        ConsoleOutputUtils.hrLineNumber( "ImportSelector.selectImports");
        return new String[]{"com.kevin.spring.components.helpBean.KevinImportSelector"};
    }

    //Aware
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        ConsoleOutputUtils.hrLineNumber("BeanFactoryAware.setBeanFactory" );
    }

    @Override
    public void setBeanName(String name) {
        ConsoleOutputUtils.hrLineNumber("BeanNameAware.setBeanName");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ConsoleOutputUtils.hrLineNumber("ApplicationContextAware.setApplicationContext");
    }

    @Override
    public void setEnvironment(Environment environment) {
        ConsoleOutputUtils.hrLineNumber("EnvironmentAware.setEnvironment");
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        ConsoleOutputUtils.hrLineNumber("BeanClassLoaderAware.setBeanClassLoader");
    }

    //Bean设置和销毁
    @Override
    public void destroy() throws Exception {
        ConsoleOutputUtils.hrLineNumber("DisposableBean.destroy");
//        ConsoleOutputUtils.hrLineNumber(" 调用销毁方法", "DisposableBean.destroy");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ConsoleOutputUtils.hrLineNumber("InitializingBean.afterPropertiesSet");
//        ConsoleOutputUtils.hrLineNumber(" 调用属性设置方法", "InitializingBean.afterPropertiesSet");
    }

    //后处理器
    public static final String KEVIN_BEAN_NAME = "kevinBean";
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
//        ConsoleOutputUtils.hrLineNumber("Bean定义注册后置处理", "BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry");
        ConsoleOutputUtils.hrLineNumber("BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry");
//        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
//                .genericBeanDefinition(KevinClass.class).getBeanDefinition();
//        registry.registerBeanDefinition(KEVIN_BEAN_NAME, beanDefinition);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//        ConsoleOutputUtils.hrLineNumber("BeanFactory后置处理", "BeanFactoryPostProcessor.postProcessBeanFactory");
        ConsoleOutputUtils.hrLineNumber("BeanFactoryPostProcessor.postProcessBeanFactory");
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
//        ConsoleOutputUtils.hrLineNumber("Bean后置处理 初始化前_" + beanName, "BeanPostProcessor.postProcessBeforeInitialization");
        ConsoleOutputUtils.hrLineNumber("BeanPostProcessor.postProcessBeforeInitialization");
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
//        ConsoleOutputUtils.hrLineNumber("Bean后置处理 初始化后_" + beanName, "BeanPostProcessor.postProcessAfterInitialization");
        ConsoleOutputUtils.hrLineNumber("BeanPostProcessor.postProcessAfterInitialization");

        return bean;
    }

}
