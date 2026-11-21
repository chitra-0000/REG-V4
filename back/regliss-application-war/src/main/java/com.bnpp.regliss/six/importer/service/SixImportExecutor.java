package com.bnpp.regliss.six.importer.service;

import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.six.importer.exception.SixImportException;
import com.bnpp.regliss.service.ReglissRequestContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class SixImportExecutor {

    private final ReglissRequestContext requestContext;
    private final String threadBaseName;
    private final Semaphore sem;
    private final ThreadPoolExecutor executor;
    private final AtomicReference<Throwable> exceptionFromWorker = new AtomicReference<>();

    public SixImportExecutor(String threadBaseName, int workerThreads, ReglissRequestContext requestContext) {
        this.requestContext = requestContext;
        this.threadBaseName = threadBaseName;
        this.sem = new Semaphore(workerThreads);
        this.executor = new ThreadPoolExecutor(workerThreads, workerThreads, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(workerThreads), new WorkerThreadFactory());
        log.debug("[{}] started: {} threads, queue {}", threadBaseName, workerThreads, workerThreads);
    }

    public void blockingSubmit(Runnable task) {
        if (exceptionFromWorker.get() != null) {
            throw finish().orElseThrow(IllegalStateException::new);
        }
        try {
            sem.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SixImportException("SIX import interrupted while waiting for a writer thread", e);
        }
        executor.execute(() -> {
            sem.release();
            boolean finished = false;
            try {
                task.run();
                finished = true;
            } catch (RuntimeException e) {
                exceptionFromWorker.compareAndSet(null, e);
                finished = true;
                log.error("[{}] worker task failed", threadBaseName, e);
            } finally {
                if (!finished) {
                    exceptionFromWorker.compareAndSet(null,
                            new SixImportException("SIX import worker stopped by a JVM error"));
                }
            }
        });
    }

    public void waitUntilFinished() {
        Optional<SixImportException> error = finish();
        if (error.isPresent()) {
            throw error.get();
        }
    }

    private Optional<SixImportException> finish() {
        executor.shutdown();
        try {
            boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
            while (!terminated) {
                log.info("[{}] still writing the last batches...", threadBaseName);
                terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SixImportException("SIX import interrupted while waiting for the writer threads", e);
        }
        Throwable error = exceptionFromWorker.get();
        return Optional.ofNullable(error).map(e -> new SixImportException("Exception occurred in SIX import worker", e));
    }

    private class WorkerThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            User callerUser = requestContext.getCurrentUser();
            Thread thread = new Thread(() -> {
                requestContext.setRequestTime(LocalDateTime.now());
                requestContext.setCurrentUser(callerUser);
                r.run();
            });
            thread.setName(threadBaseName + "-" + counter.getAndIncrement());
            thread.setUncaughtExceptionHandler((t, e) -> {
                exceptionFromWorker.compareAndSet(null, e);
                log.error("[{}] uncaught exception in worker", threadBaseName, e);
            });
            return thread;
        }
    }
}
