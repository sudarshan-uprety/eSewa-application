package com.example.helloworld.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
@Order(0) // Ensure this runs before the transaction starts
public class ReadOnlyAspect {

    @Around("@annotation(transactional)")
    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, Transactional transactional) throws Throwable {
        try {
            if (transactional.readOnly()) {
                DatabaseContextHolder.set(DataSourceType.READ);
            } else {
                DatabaseContextHolder.set(DataSourceType.WRITE);
            }
            return proceedingJoinPoint.proceed();
        } finally {
            DatabaseContextHolder.reset();
        }
    }
}
