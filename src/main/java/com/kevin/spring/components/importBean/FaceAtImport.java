package com.kevin.spring.components.importBean;

import com.kevin.spring.components.helpBean.KevinAtImportClass;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Component
@Import({KevinAtImportClass.class})
public class FaceAtImport {

}
