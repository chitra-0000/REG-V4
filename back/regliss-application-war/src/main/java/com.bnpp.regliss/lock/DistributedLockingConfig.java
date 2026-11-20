package com.bnpp.regliss.lock;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@Configuration
public class DistributedLockingConfig {
    @Bean
    DefaultLockRepository defaultLockRepository(DataSource dataSource) {
        DefaultLockRepository repository = new DefaultLockRepository(dataSource);
        repository.setTimeToLive((int) TimeUnit.HOURS.toMillis(3));
        return repository;
    }

    @Bean
    JdbcLockRegistry jdbcLockRegistry(org.springframework.integration.jdbc.lock.LockRepository repository) {
        return new JdbcLockRegistry(repository);
    }
}
