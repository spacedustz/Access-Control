package com.accesscontrol.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Slf4j
@Aspect
public class EventAspect {
    @Pointcut("execution(public void com.accesscontrol.service.EventService.*(..))")
    public void targetEvent() {}

    @Around(value = "targetEvent()")
    public Object eventAspect(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.error("Event Aspect Error - {}", e.getMessage());
        } finally {
            Signature signature = joinPoint.getSignature();
            log.info("Aspect 실행 : {}, {}", joinPoint.getTarget().getClass().getSimpleName(), signature.getName());
        }

        return result;
    }
}
