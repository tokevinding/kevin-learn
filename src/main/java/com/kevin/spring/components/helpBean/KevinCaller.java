package com.kevin.spring.components.helpBean;

import com.kevin.spring.components.FaceFactoryBean;
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
    public void init() throws Exception {
        ConsoleOutputUtils.hrl();
        ConsoleOutputUtils.hr("@PostConstruct init");
        KevinFactoryBeanClass kevinFactoryBeanClass = (KevinFactoryBeanClass)applicationContext.getBean("faceFactoryBean");
        ConsoleOutputUtils.hr("@PostConstruct init - bean self- " + kevinFactoryBeanClass.toString());
        FaceFactoryBean faceFactoryBean = (FaceFactoryBean)applicationContext.getBean("&faceFactoryBean");
        ConsoleOutputUtils.hr("@PostConstruct init - factory bean self - " + faceFactoryBean.getObject());

        ConsoleOutputUtils.hrl();

        Object bean = applicationContext.getBean(FaceBeanDefinitionRegistryPostProcessor.KEVIN_BEAN_NAME);
        ConsoleOutputUtils.hr("Bean工厂后置处理 创建的Bean： %s", bean.toString());
        KevinAtImportClass atImportClass = applicationContext.getBean(KevinAtImportClass.class);
        ConsoleOutputUtils.hr("@Import 创建的Bean： %s", atImportClass.toString());
        KevinImportSelector importSelector = applicationContext.getBean(KevinImportSelector.class);
        ConsoleOutputUtils.hr("ImportSelector 创建的Bean： %s", importSelector.toString());
        bean = applicationContext.getBean(FaceImportBeanDefinitionRegistrar.KEVIN_IMPORT_BEAN_NAME);
        ConsoleOutputUtils.hr("ImportBeanDefinitionRegistrar 创建的Bean： %s", bean.toString());
        ConsoleOutputUtils.hrl();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
