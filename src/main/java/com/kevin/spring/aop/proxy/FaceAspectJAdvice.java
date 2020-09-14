package com.kevin.spring.aop.proxy;

import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectInstanceFactory;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;

import java.lang.reflect.Method;

/**
 * Advised、Advice、Advisor的区别
 *
 * Advised:   包含所有的Advisor 和 Advice
 * Advice:    通知拦截器
 * Advisor:   通知 + 切入点的适配器
 */
public class FaceAspectJAdvice extends AbstractAspectJAdvice {
    /**
     * Create a new AbstractAspectJAdvice for the given advice method.
     *
     * @param aspectJAdviceMethod   the AspectJ-style advice method
     * @param pointcut              the AspectJ expression pointcut
     * @param aspectInstanceFactory the factory for aspect instances
     */
    public FaceAspectJAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
        super(aspectJAdviceMethod, pointcut, aspectInstanceFactory);
    }

    @Override
    public boolean isBeforeAdvice() {
        return true;
    }

    @Override
    public boolean isAfterAdvice() {
        return false;
    }
}
