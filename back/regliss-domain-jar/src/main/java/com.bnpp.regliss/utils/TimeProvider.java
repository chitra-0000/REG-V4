package com.bnpp.regliss.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeProvider {
    private static LocalDateTime testTime = null;

    public static LocalDateTime now() {
        if (testTime != null) {
            return testTime;
        } else {
            return LocalDateTime.now();
        }
    }

    static void setTestTimeStr(LocalDateTime testTime) {
        TimeProvider.testTime = testTime;
    }
    public static void clearTestTime() {
        testTime = null;
    }
}
