package com.bnpp.regliss;

import com.bnpp.regliss.dbprops.DatabaseConfigurationProvider;
import com.bnpp.regliss.dbprops.DatabaseOverridableProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.asList;

@Slf4j
public class DatabaseOverridablePropertiesListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        if (environment.acceptsProfiles("test")) {
            log.info("Skipping reading CONFIG_PARAMS from tests");
            return;
        }

        Properties configAppProps = readClasspathProps("/config-app.properties");
        environment.getPropertySources().addFirst(new PropertiesPropertySource("config-app", configAppProps));


        Properties configAppLocalProps = readLocalProps();
        checkNoExtraLocalKeys(configAppProps, configAppLocalProps);
        environment.getPropertySources().addFirst(new PropertiesPropertySource("config-app-local", configAppLocalProps));

        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        SingleConnectionDataSource dataSource = createSingleConnectionDataSource(url, username, password);
        Properties propsViaDB = overridePropertiesFromDatabase(dataSource, configAppProps, configAppLocalProps);
        environment.getPropertySources().addFirst(new PropertiesPropertySource("db-props", propsViaDB));

        // AER_LIMA_1799 : to change java io default folder to be application tmp folder instead of default system file
        // and to avoid that CFT folder be used for other needs.
        String tempFileFolder = environment.getProperty("temp.file.folder");
        System.setProperty("java.io.tmpdir", tempFileFolder);
    }

    private Properties readLocalProps() {
        Properties configAppLocalProps;
        if (ReglissEnvironment.isLocalDev()) {
            configAppLocalProps = readClasspathProps("/config-app-local.properties");
        } else {
            configAppLocalProps = new Properties();
        }
        return configAppLocalProps;
    }

    private void checkNoExtraLocalKeys(Properties configAppProps, Properties configAppLocalProps) {
        Set<String> localKeys = configAppLocalProps.stringPropertyNames();
        Set<String> dbOverridableKeys = configAppProps.stringPropertyNames();

        Set<String> localKeysNotInDB = minus(localKeys, dbOverridableKeys);
        localKeysNotInDB.removeIf(s -> s.startsWith("spring.datasource.")); // ignore DB connection props
        if (!localKeysNotInDB.isEmpty()) {
            throw new IllegalArgumentException(
                    "Extra properties found in config-app-local.properties that are not in application.properties: " + new TreeSet<>(localKeysNotInDB));
        }
    }

    private Set<String> minus(Set<String> from, Set<String> what) {
        Set<String> result = new HashSet<>(from);
        result.removeAll(what);
        return result;
    }

    private Properties readClasspathProps(String path) {
        try (InputStream propsStream = ServerConfig.class.getResourceAsStream(path)) {
            if (propsStream == null) {
                throw new IOException("Could not load file");
            }
            Properties props = new Properties();
            props.load(propsStream);
            return props;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load properties files: " + path,e);
        }
    }

    private SingleConnectionDataSource createSingleConnectionDataSource(String url, String username, String password) {
        return new SingleConnectionDataSource(url, username, password, true);
    }

    public Properties overridePropertiesFromDatabase(DataSource dataSource, Properties configAppProps, Properties configAppLocalProps) {
        log.info("Initializing database overridable properties with datasource: {}", dataSource);
        DatabaseOverridableProperties databaseProperties = new DatabaseOverridableProperties();
        databaseProperties.setDatabaseConfigurationProvider(new DatabaseConfigurationProvider(dataSource));
        databaseProperties.setDefaultProperties(asList(configAppProps, configAppLocalProps));
        return databaseProperties.getProperties();
    }
}

