package com.bnpp.regliss.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PerformanceUtil {
    private static Logger log = LoggerFactory.getLogger(PerformanceUtil.class);

    public static <T> T runAndMeasure(String label, Supplier<T> toMeasure) {
        long t0 = System.currentTimeMillis();
        try {
            return toMeasure.get();
        } finally {
            log.debug("Execution of {} took {}", label, formatDeltaTime(System.currentTimeMillis() - t0));
        }
    }
    public static void runAndMeasure(String label, Runnable toMeasure) {
        runAndMeasure(label, () -> {toMeasure.run(); return 1;});
    }

    public static String formatDeltaTime(long millis) {
        return String.format("%.03f sec", millis / 1000f);
    }

}
