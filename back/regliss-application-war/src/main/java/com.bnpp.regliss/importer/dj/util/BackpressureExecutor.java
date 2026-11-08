package com.bnpp.regliss.importer.dj.util;

// [MISSING FROM SCREENSHOTS: lines 3-11 of the original file were not visible in the photos]

public class BackpressureExecutor {
    private static final Logger log = LoggerFactory.getLogger(BackpressureExecutor.class);

    private final ReglissRequestContext requestContext;
    private final String threadBaseName;

    private final Semaphore sem;
    private final ThreadPoolExecutor executor;
    private Throwable exceptionFromWorker;

    private TimeStamps timeStamps = new TimeStamps();

    public BackpressureExecutor(String threadBaseName, int workerThreads, ReglissRequestContext requestContext) {
        this.requestContext = requestContext;
        this.threadBaseName = threadBaseName;

        sem = new Semaphore(workerThreads);
        executor = new ThreadPoolExecutor(workerThreads, workerThreads, 0,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(workerThreads), new MyThreadFactory());
        log.debug("Started thread pool: threads: {} , queue size: {}", workerThreads, workerThreads);
    }

    private class MyThreadFactory implements ThreadFactory {
        private int counter;

        @Override
        public Thread newThread(Runnable r) {
            r = propagateRequestContext(r);
            r = collectStatsAtEnd(r);
            Thread thread = new Thread(r);
            thread.setName(threadBaseName + "-" + counter);
            thread.setUncaughtExceptionHandler((Thread t, Throwable exception) -> {
                exceptionFromWorker = exception;
                log.debug("Caught exception in worker: " + exception, exception);
            });
            counter++;
            return thread;
        }

        private Runnable collectStatsAtEnd(Runnable r) {
            return () -> {
                r.run();
                synchronized (BackpressureExecutor.this) {
                    timeStamps.add(PerformanceMonitor.current());
                    log.debug("Worker stopped. Collected its performance stats.");
                }
            };
        }

        public Runnable propagateRequestContext(Runnable runnable) {
            log.debug("Propagating from thread with user id = {}", requestContext.getUserId());
            User callerUser = requestContext.getCurrentUser();
            return () -> {
                requestContext.setRequestTime(LocalDateTime.now());
                requestContext.setCurrentUser(callerUser);
                log.debug("Restored user id {} on child worker thread", callerUser.getId());
                runnable.run();
            };
        }
    }


    public void blockingSubmit(Runnable task) {
        if (exceptionFromWorker != null) {
            log.warn("Will throw back exception from worker after all other workers stop: ", exceptionFromWorker);
            shutdownAndWaitToDie();
            log.warn("Throwing back exception from worker: ", exceptionFromWorker);
            throw new RuntimeException("Exception occured in worker", exceptionFromWorker);
        }
        try {
            sem.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        task = new RunnableReleasingSemaphore(task);
        executor.execute(task);
    }

    private class RunnableReleasingSemaphore implements Runnable {
        private final Runnable runnable;

        public RunnableReleasingSemaphore(Runnable runnable) { this.runnable = runnable; }

        public void run() {
            sem.release();
            runnable.run();
        }

    }

    public void shutdownAndWaitToDie() {
        log.debug("Awaiting termination of tasks: running: {}, in queue:{}", executor.getActiveCount(), executor.getQueue().size());
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        log.debug("UPDATE executor shut down");
        log.debug("EXECUTOR ({} threads): {}", executor.getCorePoolSize(), timeStamps);
        if (exceptionFromWorker != null) {
            log.debug("Worker thread threw exception. Throwing it back in the main thread...");
            throw new RuntimeException("Exception occured worker", exceptionFromWorker);
        }
}

}
