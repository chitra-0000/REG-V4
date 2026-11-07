package com.bnpp.regliss.config.dbprops;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import static java.lang.String.format;

public class DatabaseConfigurationProvider  {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfigurationProvider.class);

    public static final String TABLE_NAME = "CONFIG_PARAMETERS";
    public static final String TABLE_COLUMN_PROPERTY = "KEY";
    public static final String TABLE_COLUMN_VALUE = "VALUE";
    public static final String TABLE_COLUMN_DEFAULT_VALUE = "DEFAULT_VALUE";

    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactionManager;

    public DatabaseConfigurationProvider(DataSource dataSource) {
        LOGGER.info("Using table structure: {}({},{},{})", TABLE_NAME, TABLE_COLUMN_PROPERTY, TABLE_COLUMN_VALUE, TABLE_COLUMN_DEFAULT_VALUE);
        this.jdbc = new JdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        createConfigParamsTableIfNecessary();
    }

    private void createConfigParamsTableIfNecessary() {
        DataSource dataSource =  jdbc.getDataSource();
        if (dataSource == null) {
            throw new IllegalStateException("Could not perform database operation : DataSource is null");
        } else {
            try (Connection connection = dataSource.getConnection()) {
                if (connection == null) {
                    throw new IllegalStateException("Could not perform database operation : DataSource connection is null");
                }
                //TODO this method returns always true on dev server, could not figure out why, use this method when we find out
                /*ResultSet result = metaData.getTables(null, null, TABLE_NAME, new String[]{"TABLE"})*/
                SqlRowSet rowSet = jdbc.queryForRowSet(format("SELECT * FROM USER_TABLES WHERE TABLE_NAME = '%s'", TABLE_NAME));
                if (!rowSet.next()) {
                    createTable();
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Could not perform database operation", e);
            }
        }
    }

    private void createTable() {
        LOGGER.debug("{} TABLE inexistent. Creating table {}", TABLE_NAME, TABLE_NAME);
        jdbc.execute(format(
            "CREATE TABLE %s (%s VARCHAR2(50 CHAR) NOT NULL, %s VARCHAR2(2048 CHAR), %s VARCHAR2(2048 CHAR))",
            TABLE_NAME, TABLE_COLUMN_PROPERTY, TABLE_COLUMN_VALUE, TABLE_COLUMN_DEFAULT_VALUE)
        );
    }

    public Properties resolveProperties(Properties defaultProperties) {
        setDefaultProperties(defaultProperties);
        return getResolvedProperties(jdbc);
    }

    private void setDefaultProperties(final Properties defaultProperties) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.execute(status -> {
            doPersistDefaultProperties(defaultProperties);
            return null;
        });
    }

    private void doPersistDefaultProperties(Properties defaultProperties) {
        LOGGER.debug("Inserting default values {} ", defaultProperties);
        String insertSql = format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                TABLE_NAME, TABLE_COLUMN_PROPERTY, TABLE_COLUMN_DEFAULT_VALUE);
        String updateSql = format("UPDATE %s SET %s = ? WHERE %s = ?",
                TABLE_NAME, TABLE_COLUMN_DEFAULT_VALUE, TABLE_COLUMN_PROPERTY);
        LOGGER.trace("Using update SQL: {}", updateSql);
        LOGGER.trace("Using insert SQL: {}", insertSql);
        int insertedCount = 0;
        int updatedCount = 0;
        for (String propertyName: defaultProperties.stringPropertyNames()) {
            String defaultValue = defaultProperties.getProperty(propertyName);

            if (propertyExistsInDatabase(propertyName)) {
                jdbc.update(updateSql, defaultValue, propertyName);
                updatedCount ++;
            } else {
                jdbc.update(insertSql, propertyName, defaultValue);
                insertedCount ++;
            }
        }
        LOGGER.debug("Inserted {} new rows, Updated {} rows", insertedCount, updatedCount);
    }

    private boolean propertyExistsInDatabase(String propertyName) {
        String sql = format("SELECT COUNT(*) FROM %s WHERE %s = ?", TABLE_NAME, TABLE_COLUMN_PROPERTY);
        if (jdbc != null) {
            return 1 == jdbc.queryForObject(sql, Integer.class, propertyName);
        } else {
            return false;
        }
    }


    public static Properties getResolvedProperties(JdbcTemplate jdbc) {
        final Properties properties = new Properties();
        String sql = format("SELECT %s, %s, %s FROM %s",
                    TABLE_COLUMN_PROPERTY, TABLE_COLUMN_VALUE, TABLE_COLUMN_DEFAULT_VALUE, TABLE_NAME);
        jdbc.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                String propertyName = rs.getString(1);
                String propertyValue = rs.getString(2);
                String propertyDefaultValue = rs.getString(3);

                String value = StringUtils.isNotBlank(propertyValue) ? propertyValue : propertyDefaultValue;
                if (value != null) {
                    properties.setProperty(propertyName, value);
                }
            }
        });
        return properties;
    }

    public String getTableName() { return TABLE_NAME; }

}

