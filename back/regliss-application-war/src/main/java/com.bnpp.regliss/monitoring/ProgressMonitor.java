package com.bnpp.regliss.monitoring;

import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.lang.System.currentTimeMillis;

public class ProgressMonitor {

    private static final long PRINT_INTERVAL_SECONDS = 10;

    private Long startTimeMillis;
    private long lastPrintTimeMillis;
    private long lastPrintTimeCount;
    private long count;
    private Long totalCount;

    private final String label;
    private final Logger log;

    public ProgressMonitor(String label, Logger log) { this(label, log, null); }

    public ProgressMonitor(String label, Logger log, long totalCount) { this(label, log, (Long)totalCount); }
    public ProgressMonitor(String label, Logger log, Long totalCount) {
        this.log = log;
        this.totalCount = totalCount;
        this.startTimeMillis = System.currentTimeMillis();
        this.label=label;
    }

    public void end() { log.debug("FINAL {}", generateStats(System.currentTimeMillis())); }

    public void add(int deltaCount) {
        count+=deltaCount;
        if (startTimeMillis == null) {
            startTimeMillis = System.currentTimeMillis();
            lastPrintTimeMillis = System.currentTimeMillis();

            log.debug("STATS ({}): Monitoring processing speed.", label);
        }
        long time = System.currentTimeMillis();
        if (time - lastPrintTimeMillis > PRINT_INTERVAL_SECONDS * 1000) {
            log.debug(generateStats(time));
        }
    }

    public long getCount() { return count; }

    private String generateStats(long time) {
        float avgSpeed = 1000f*(count)/(time - startTimeMillis);


        String s = "STATS "+label+": ";

        if (totalCount != null) {
            s += String.format("%.1f%%;", 100f * count / totalCount);
        }
        s += String.format(" SPEED: %.1f/s", avgSpeed);

        if (totalCount != null) {
            s += String.format(" ITEMS: %,d of %,d;", count, totalCount);
        } else {
            s += String.format(" ITEMS: %,d;", count);
        }


        s += " TIME Elapsed: " + timeElapsedStr(time-startTimeMillis);
        if (totalCount != null) {
            s += computeEstimated(avgSpeed);
        }

        s += String.format("; DELTA: %d over last %.1f s",
                count - lastPrintTimeCount,
                (time - lastPrintTimeMillis)/1000f
                );


        lastPrintTimeMillis = time;
        lastPrintTimeCount = count;
        return s;
    }

    private static String timeElapsedStr(long millis) {
        int totalSeconds = (int) (millis / 1000);
        int seconds = totalSeconds % 60;
        int minutes = totalSeconds / 60;
        int hours = minutes / 60;
        minutes %= 60;
        if (hours >= 1) {
            return String.format("%d:%02d:%02d", hours,minutes, seconds);
        } else {
            return String.format("%02d:%02d",minutes, seconds);
        }
    }


    private String computeEstimated(float avgSpeed) {
        int itemsLeft = (int) (totalCount-count);
        int secondsLeftEst = (int) (itemsLeft / avgSpeed);
        int totalSecondsEst = (int) (secondsLeftEst*1000 + (currentTimeMillis() - startTimeMillis));
        LocalDateTime eta = LocalDateTime.now().plusSeconds(secondsLeftEst);
        return ", Left: ~" +timeElapsedStr(secondsLeftEst * 1000L)+
                ", Finish at: ~" +DateTimeFormatter.ofPattern("HH:mm").format(eta) + ", Total: ~" + timeElapsedStr(totalSecondsEst);
    }

}

