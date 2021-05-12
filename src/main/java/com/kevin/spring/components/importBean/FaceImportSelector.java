package com.kevin.spring.components.importBean;

import com.kevin.spring.components.CountThreadLocal;
import com.kevin.tools.utils.ConsoleOutputUtils;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;

@Component
public class FaceImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        ConsoleOutputUtils.hrLineNumber("FaceImportSelector.selectImports");
        return new String[]{"com.kevin.spring.components.helpBean.KevinImportSelector"};
    }
}
