package com.bnpp.regliss;

public class ReglissEnvironment {

    private ReglissEnvironment() {
        //
    }

    public static boolean isLocalDev() {
        return "local".equalsIgnoreCase(System.getProperty("regliss.env", "unknown"));
    }

    private static Boolean isJUnitTest;
    public static synchronized boolean isJUnitTest() {
        if (isJUnitTest == null) {
            isJUnitTest = determineIfInJUnit();
        }
        return isJUnitTest;
    }

    private static boolean determineIfInJUnit() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;
    }
}
