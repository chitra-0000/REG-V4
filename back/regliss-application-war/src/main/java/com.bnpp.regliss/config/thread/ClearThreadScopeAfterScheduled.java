package com.bnpp.regliss.config.thread;

// [MISSING FROM SCREENSHOTS: lines 3-8 of the original file were not visible in the photos]

@Slf4j
@Component
@Aspect
public class ClearThreadScopeAfterScheduled {
    @After("execution(* *(..)) && @annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void clearThreadScope(JoinPoint point) {
        log.trace("Clearing thread scope after method {}", point.getSignature().getName());
        ThreadScopeContextHolder.clearThread();
    }
}
