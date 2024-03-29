package com.kevin.spring.components.postProcessor;

import com.kevin.spring.components.CountThreadLocal;
import com.kevin.spring.components.helpBean.KevinClass;
import com.kevin.spring.components.helpBean.KevinPostClass;
import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;

/**
 * BeanDefinitionRegistryPostProcessor 接口可以看作是BeanFactoryPostProcessor
 * 和ImportBeanDefinitionRegistrar的功能集合，既可以获取和修改BeanDefinition的元数据
 * ，也可以实现BeanDefinition的注册、移除等操作。
 **/
@Order(999)
//@Component
public class FaceInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return new KevinPostClass();
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        ConsoleOutputUtils.hr(CountThreadLocal.incrementAndGet() + "FaceInstantiationAwareBeanPostProcessor.postProcessAfterInstantiation");
        return false;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        ConsoleOutputUtils.hr(CountThreadLocal.incrementAndGet() + "FaceInstantiationAwareBeanPostProcessor.postProcessPropertyValues");
        return null;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ConsoleOutputUtils.hr(CountThreadLocal.incrementAndGet() + "FaceInstantiationAwareBeanPostProcessor.postProcessBeforeInitialization");
        return null;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        ConsoleOutputUtils.hr(CountThreadLocal.incrementAndGet() + "FaceInstantiationAwareBeanPostProcessor.postProcessAfterInitialization");
        return null;
    }
}
