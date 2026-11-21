package com.bnpp.regliss.six.exporter.service;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public final class SixKeepAliveTimer implements AutoCloseable {

    private final ScheduledExecutorService executor;
    private final String name;

    private SixKeepAliveTimer(String name, long periodSeconds, long maxMinutes, Runnable action) {
        this.name = name;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "six-keepalive-" + name);
            t.setDaemon(true);
            return t;
        });
        long period = Math.max(1, periodSeconds);
        long maxMillis = TimeUnit.MINUTES.toMillis(maxMinutes);
        long start = System.currentTimeMillis();
        executor.scheduleAtFixedRate(() -> tick(action, start, maxMillis), period, period, TimeUnit.SECONDS);
    }

    public static SixKeepAliveTimer start(String name, long periodSeconds, long maxMinutes, Runnable action) {
        return new SixKeepAliveTimer(name, periodSeconds, maxMinutes, action);
    }

    private void tick(Runnable action, long start, long maxMillis) {
        if (System.currentTimeMillis() - start > maxMillis) {
            log.warn("Keep-alive of {} stopped after {} minutes: the step is still running, it is not refreshed any more",
                    name, TimeUnit.MILLISECONDS.toMinutes(maxMillis));
            executor.shutdown();
            return;
        }
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("Keep-alive of {} failed (next try in the next tick): {}", name, e.getMessage());
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.debug("Keep-alive of {}: last refresh still running", name);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


