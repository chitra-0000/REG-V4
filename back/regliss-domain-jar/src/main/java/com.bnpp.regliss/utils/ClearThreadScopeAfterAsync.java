package com.bnpp.regliss.utils;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.bnpp.regliss.utils.thread.ThreadScopeContextHolder;

import lombok.extern.slf4j.Slf4j;

@Component
@Aspect
@Slf4j
public class ClearThreadScopeAfterAsync implements Ordered {

    @After("execution(* *(..)) && @annotation(org.springframework.scheduling.annotation.Async)")
    public void clearThreadScope() {
        log.debug("Clearing Thread Scoped beans from thread {}", Thread.currentThread().getName());
        ThreadScopeContextHolder.currentThreadScopeAttributes().clear();
    }

    @Override
    public int getOrder() { return -10; // Before Resource Closer, and before Transaction AOP }
}
