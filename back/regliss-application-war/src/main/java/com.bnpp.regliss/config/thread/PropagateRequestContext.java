package com.bnpp.regliss.config.thread;

// [MISSING FROM SCREENSHOTS: lines 3-10 of the original file were not visible in the photos]

@Slf4j
@Component
public class PropagateRequestContext implements TaskDecorator {
    @Autowired
    private ReglissRequestContext requestContext;

    public Runnable decorate(Runnable runnable) {
        log.debug("Decorating from thread with user id = " + requestContext.getUserId());
        User callerUser = requestContext.getCurrentUser();
        return () -> {
            requestContext.setRequestTime(LocalDateTime.now());
            requestContext.setCurrentUser(callerUser); //set on the async thread (different )
            log.debug("Restored user id {} on thread", callerUser.getId());
            runnable.run();
        };
    }
}
