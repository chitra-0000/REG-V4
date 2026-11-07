package com.bnpp.regliss.config.thread.autocloseable;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@Aspect
@Slf4j
public class CloseResourcesAfterAspect implements Ordered {

    @Autowired
    private CloseableThreadResources resourcesToClose;

    @Around("execution(* *(..)) && @annotation(com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter) ")
    public Object closeResourcesAfter(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } finally {
            closeAll();
            deleteAll();
        }
    }

    private void deleteAll() {
        for (File file : resourcesToClose.getFilesToDelete()) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                log.warn("Could not delete file {}", file.getAbsolutePath(), e);
            }
        }
        log.debug("Attempted to delete {} files", resourcesToClose.getFilesToDelete().size());
        resourcesToClose.getFilesToDelete().clear();
    }

    private void closeAll() {
        for (Closeable c : resourcesToClose.getToClose()) {
            try {
                c.close();
            } catch (IOException e) {
                log.warn("Could not close stream!");
            }
        }
        log.debug("Closed {} streams", resourcesToClose.getToClose().size());
        resourcesToClose.getToClose().clear();
    }

    @Override
    public int getOrder() {
        return 0; // DON'T CHANGE: IT's CRITICAL TO RUN BEFORE @Transactional
    }

}

