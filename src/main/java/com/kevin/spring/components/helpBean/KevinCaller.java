package com.kevin.spring.components.helpBean;

import com.kevin.spring.components.importBean.FaceImportBeanDefinitionRegistrar;
import com.kevin.spring.components.importBean.FaceImportSelector;
import com.kevin.spring.components.postProcessor.FaceBeanDefinitionRegistryPostProcessor;
import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Import({FaceImportBeanDefinitionRegistrar.class, FaceImportSelector.class})
public class KevinCaller implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        Object bean = applicationContext.getBean(FaceBeanDefinitionRegistryPostProcessor.KEVIN_BEAN_NAME);
        ConsoleOutputUtils.hr("Bean工厂后置处理 创建的Bean： %s", bean.toString());
        KevinAtImportClass atImportClass = applicationContext.getBean(KevinAtImportClass.class);
        ConsoleOutputUtils.hr("@Import 创建的Bean： %s", atImportClass.toString());
        KevinImportSelector importSelector = applicationContext.getBean(KevinImportSelector.class);
        ConsoleOutputUtils.hr("ImportSelector 创建的Bean： %s", importSelector.toString());
        bean = applicationContext.getBean(FaceImportBeanDefinitionRegistrar.KEVIN_IMPORT_BEAN_NAME);
        ConsoleOutputUtils.hr("ImportBeanDefinitionRegistrar 创建的Bean： %s", bean.toString());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
