package com.bnpp.regliss.config.controller;

import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.utils.PerformanceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Slf4j
@Aspect
@Component
public class ExternalAPILoggingInterceptor {

    @Autowired
    private ReglissRequestContext requestConext;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void configureMapper() {
        if (log.isTraceEnabled()) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }

    @Around("@within(com.bnpp.regliss.config.controller.ExternalAPILog) || @annotation(com.bnpp.regliss.config.controller.ExternalAPILog)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        logBefore(joinPoint);
        long t0 = System.currentTimeMillis();

        Object returnedObject;
        try {
            returnedObject = joinPoint.proceed();
        } catch (Exception e) {
            long deltaMillis = System.currentTimeMillis() - t0;
            log.error("Exception ({}): {}: {}",
                    PerformanceUtil.formatDeltaTime(deltaMillis),
                    e.getClass(), e.getMessage());
            throw e;
        }

        logAfter(joinPoint, t0);
        return returnedObject;
    }

    private void logBefore(ProceedingJoinPoint joinPoint) {
        if (log.isInfoEnabled()) {
            log.info("[CorrID: {}] Invoking {}.{}({}) for user {}",
                    getCorrelationId(joinPoint),
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    getActualParams(joinPoint),
                    requestConext.getUsername());
        }
    }

    private void logAfter(ProceedingJoinPoint joinPoint, long t0) {
        if (log.isInfoEnabled()) {
            long deltaMillis = System.currentTimeMillis() - t0;
            log.info("[CorrID: {}] Return from {}.{}({}) ({}) for user {}",
                    getCorrelationId(joinPoint),
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    getActualParams(joinPoint),
                    PerformanceUtil.formatDeltaTime(deltaMillis),
                    requestConext.getUsername());
        }
    }

    private String getActualParams(ProceedingJoinPoint joinPoint) {
        String separator = log.isTraceEnabled() ? "\n" : ", ";
        return Stream.of(joinPoint.getArgs())
                .filter(object -> !(object instanceof HttpServletRequest))
                    .map(this::objectToJson)
                    .map(Object::toString)
                    .collect(joining(separator));
    }

    private String getCorrelationId(ProceedingJoinPoint joinPoint) {
        return Stream.of(joinPoint.getArgs())
                .filter(HttpServletRequest.class::isInstance).findFirst()
                .map(req -> ((HttpServletRequest)req).getHeader("x-bnpp-correlation-id"))
                .orElse("");
    }

    private String objectToJson(Object object) {
        try {
            if (object instanceof ServletOutputStream) {
                return "<ServletOutputStream>";
            } else if (object instanceof InputStream) {
                return "<InputStream>";
            } else {
                return objectMapper.writeValueAsString(object);
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize as JSON (stacktrace on TRACE): ", e);
            log.trace("Cannot serialize value: " , e);

            return "<ERR>";
        }
    }

}

