package com.bnpp.regliss.dbprops;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
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

        Properties dbProps = databaseConfigurationProvider.resolveProperties(mergedProps);
        Properties dbOverridenProps = filterOutExtraDBKeys(dbProps, mergedProps); // NOSONAR: more readable
        return dbOverridenProps;
    }

    private Properties filterOutExtraDBKeys(Properties dbProps, Properties fileProps) {
        Set<String> dbExtraKeys = new HashSet<>(dbProps.stringPropertyNames());
        dbExtraKeys.removeAll(fileProps.stringPropertyNames());
        if (!dbExtraKeys.isEmpty()) {
            log.warn("Ignoring extra {} properties present in DB but not in files: {}", dbExtraKeys.size(), dbExtraKeys);
        }
        Properties filteredProps = new Properties();
        for (String key : fileProps.stringPropertyNames()) {
            filteredProps.setProperty(key, dbProps.getProperty(key, fileProps.getProperty(key)));
        }
        return filteredProps;
    }

    public void setDefaultProperties(List<Properties> defaultProperties) { this.defaultProperties = defaultProperties; }

    public void setDatabaseConfigurationProvider(DatabaseConfigurationProvider databaseConfigurationProvider) {
        this.databaseConfigurationProvider = databaseConfigurationProvider;
    }
}

