package com.bnpp.regliss;

public class PropertiesLocationResolver {

    private PropertiesLocationResolver() {
        throw new UnsupportedOperationException("Only static methods should be invoked");
    }

    public static String resolveConfigPropertiesPathBasedOnEnvironment() {
        if (ReglissEnvironment.isLocalDev()) {
            return "/config-app-local.properties";
        } else {
            return "/config-app.properties";
        }
    }
}

