package com.kevin.design.structure.proxy.cglib.technical;

import com.kevin.design.structure.proxy.cglib.technical.clazz.OtherSampleBean;
import com.kevin.design.structure.proxy.cglib.technical.clazz.SampleBean;
import net.sf.cglib.beans.BeanCopier;
import net.sf.cglib.core.Converter;

/**
 * @author kevin
 * @date 2020-08-11 17:35:21
 * @desc
 */
public class BeanCopierTest {

    public static void main(String[] args) {
        ////设置为true，则使用converter
        BeanCopier copier = BeanCopier.create(SampleBean.class, OtherSampleBean.class, false);
        SampleBean myBean = new SampleBean();
        myBean.setValue("Hello cglib");
        OtherSampleBean otherBean = new OtherSampleBean();

        //设置为true，则传入converter指明怎么进行转换
        Converter converter = null;
        copier.copy(myBean, otherBean, converter);

        System.out.println(otherBean.getValue());
    }
}