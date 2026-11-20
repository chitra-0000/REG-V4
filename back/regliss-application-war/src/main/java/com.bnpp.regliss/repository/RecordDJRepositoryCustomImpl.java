package com.bnpp.regliss.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

public class RecordDJRepositoryCustomImpl implements RecordDJRepositoryCustom {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public void batchUpdateCsvSnippets(Map<Long, String> recordDJIdToCsvSnippet) {
        ArrayList<Map.Entry<Long, String>> csvSnippetsBatchParams = new ArrayList<>(recordDJIdToCsvSnippet.entrySet());

        jdbcTemplate.batchUpdate("UPDATE RECORD_DJ SET CSV_SNIPPET = ? WHERE ID = ?", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setString(1, csvSnippetsBatchParams.get(i).getValue());
                preparedStatement.setLong(2, csvSnippetsBatchParams.get(i).getKey());
            }

            @Override
            public int getBatchSize() { return csvSnippetsBatchParams.size(); }
        });
    }
}
