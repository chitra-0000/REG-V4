package com.bnpp.regliss;

import com.bnpp.regliss.config.thread.PropagateRequestContext;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.Collections.singleton;
@Slf4j
@Configuration
@Profile("server")
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = "com.bnpp.regliss", excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes= RestController.class))
public class ServerConfig implements AsyncConfigurer, SchedulingConfigurer {

    @Value("${scheduler.thread.count:4}")
    private int schedulerThreadCount;
    @Value("${async.executor.thread.count:1}")
    private int asyncThreadCount;

    @Autowired
    private PropagateRequestContext propagateRequestContext;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(schedulerExecutor());
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor asyncExecutor = new ThreadPoolTaskExecutor();
        asyncExecutor.setCorePoolSize(asyncThreadCount);
        asyncExecutor.setMaxPoolSize(asyncThreadCount);
        asyncExecutor.setQueueCapacity(100);
        asyncExecutor.setThreadNamePrefix("async-");
        asyncExecutor.setTaskDecorator(propagateRequestContext);
        asyncExecutor.initialize();
        return asyncExecutor;
    }

    @Bean
    @Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public ReglissRequestContext reglissRequestContext() {
        return new ReglissRequestContext();
    }

    @Bean
    public ThreadPoolTaskExecutor exportExecutor(@Value("${export.executor.thread.count:2}") int threadCount) {
        ThreadPoolTaskExecutor asyncExecutor = new ThreadPoolTaskExecutor();
        asyncExecutor.setCorePoolSize(threadCount);
        asyncExecutor.setMaxPoolSize(threadCount);
        asyncExecutor.setQueueCapacity(100);
        asyncExecutor.setThreadNamePrefix("export-");
        asyncExecutor.setTaskDecorator(propagateRequestContext);
        asyncExecutor.initialize();
        return asyncExecutor;
    }

    @Bean
    public ThreadPoolTaskExecutor manualImportExecutor(@Value("${manual.import.executor.thread.count:3}") int threadCount) {
        ThreadPoolTaskExecutor asyncExecutor = new ThreadPoolTaskExecutor();
        asyncExecutor.setCorePoolSize(threadCount);
        asyncExecutor.setMaxPoolSize(threadCount);
        asyncExecutor.setQueueCapacity(100);
        asyncExecutor.setThreadNamePrefix("saimport-");
        asyncExecutor.initialize();
        return asyncExecutor;
    }

    @Bean(destroyMethod="shutdown")
    public ScheduledExecutorService schedulerExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setThreadFactory(Executors.defaultThreadFactory())
            .setNameFormat("sched-%d")
            .build();
        return Executors.newScheduledThreadPool(schedulerThreadCount, threadFactory);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    @Bean
    public SimpleCacheManager referenceCache() {
        log.debug("Creating the list and record reference cache");
        SimpleCacheManager cache = new SimpleCacheManager();
        cache.setCaches(singleton(new ConcurrentMapCache("listAndRecordReferential", true)));
        return cache;
    }

    @Bean
    public SimpleCacheManager referenceMapCache() {
        log.debug("Creating the list and record reference map cache");
        SimpleCacheManager cache = new SimpleCacheManager();
        cache.setCaches(singleton(new ConcurrentMapCache("listAndRecordReferentialMap", true)));
        return cache;
    }
    @Bean
    public SimpleCacheManager versionProgramLabelsCache() {
        log.debug("Creating the version Program Labels cache");
        SimpleCacheManager cache = new SimpleCacheManager();
        cache.setCaches(singleton(new ConcurrentMapCache("versionProgramLabels", true)));
        return cache;
    }

    @Bean
    public SimpleCacheManager reflectionCache() {
        log.debug("Creating the reference cache");
        SimpleCacheManager cache = new SimpleCacheManager();
        cache.setCaches(singleton(new ConcurrentMapCache("reflection", true)));
        return cache;
    }


    @Bean
    public SimpleCacheManager listPermissionMatrixCache() {
        SimpleCacheManager cache = new SimpleCacheManager();
        cache.setCaches(singleton(new ConcurrentMapCache("listPermissionMatrix", true)));
        return cache;
    }

    @Bean
    @Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public SimpleCacheManager threadCaches() {
        log.debug("Creating a new thread-scoped cache");
        SimpleCacheManager cache = new SimpleCacheManager();
        cache.setCaches(singleton(new ConcurrentMapCache("threadCache", true)));
        return cache;
    }
    @Bean
    @Primary
    public CompositeCacheManager cacheManager() {
        return new CompositeCacheManager(
            referenceCache(),
            referenceMapCache(),
            listPermissionMatrixCache(),
            threadCaches(),
            reflectionCache(),
            versionProgramLabelsCache()
        );
    }
}

