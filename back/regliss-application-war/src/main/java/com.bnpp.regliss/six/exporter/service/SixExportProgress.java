package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.batch.service.BatchProgressService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class SixExportProgress {

    public static final double START_PERCENT = 5.0;
    public static final double LISTS_PERCENT = 90.0;
    public static final double AUTOMATIC_MAX = START_PERCENT + LISTS_PERCENT;
    public static final double FILTER_PART = 0.30;
    public static final double DELETE_PART = 0.05;
    public static final double WRITE_PART = 0.45;
    public static final double XML_PART = 0.20;

    private static final long KEEP_ALIVE_SECONDS = 60L;

    private final BatchProgressService batchProgressService;
    private final Long batchJobExecutionId;
    private double pending;
    private int sent;
    private ScheduledExecutorService keepAlive;

    public SixExportProgress(BatchProgressService batchProgressService, Long batchJobExecutionId) {
        this.batchProgressService = batchProgressService;
        this.batchJobExecutionId = batchJobExecutionId;
    }

    public static double listShare(int lists) {
        return lists <= 0 ? 0 : LISTS_PERCENT / lists;
    }

    public synchronized void add(double percent) {
        if (percent > 0) {
            pending += percent;
        }
        int whole = Math.min((int) pending, (int) AUTOMATIC_MAX - sent);
        if (whole > 0) {
            pending -= whole;
            sent += whole;
            push(whole);
        }
    }

    public synchronized void startKeepAlive() {
        if (keepAlive != null || batchJobExecutionId == null) {
            return;
        }
        keepAlive = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "six-export-keepalive-" + batchJobExecutionId);
            t.setDaemon(true);
            return t;
        });
        keepAlive.scheduleAtFixedRate(() -> push(0), KEEP_ALIVE_SECONDS, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS);
    }

    public synchronized void stopKeepAlive() {
        if (keepAlive != null) {
            keepAlive.shutdownNow();
            keepAlive = null;
        }
    }

    private void push(int percent) {
        if (batchJobExecutionId == null) {
            return;
        }
        try {
            batchProgressService.incrementExportBatchForSix(batchJobExecutionId, percent);
        } catch (RuntimeException e) {
            log.warn("Could not update the progress of job {}: {}", batchJobExecutionId, e.getMessage());
        }
    }
}

