package com.bnpp.regliss.config.dbprops;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class DatabaseOverridableProperties {

    private DatabaseConfigurationProvider databaseConfigurationProvider;

    private List<Properties> defaultProperties = Collections.emptyList();

    public Properties getProperties() {
        Properties mergedProps = new Properties();
        for (Properties properties: defaultProperties) {
            for (String propertyName : properties.stringPropertyNames()) {
                mergedProps.setProperty(propertyName, properties.getProperty(propertyName));
            }
        }
        return databaseConfigurationProvider.resolveProperties(mergedProps);
    }

    public void setDefaultProperties(List<Properties> defaultProperties) { this.defaultProperties = defaultProperties; }

    public void setDatabaseConfigurationProvider(DatabaseConfigurationProvider databaseConfigurationProvider) {
        this.databaseConfigurationProvider = databaseConfigurationProvider;
    }
}

