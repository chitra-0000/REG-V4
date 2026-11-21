package com.bnpp.regliss.web.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@Component
public class ApplicationRestarter {

    private static final Logger log = LoggerFactory.getLogger(ApplicationRestarter.class);

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private ConfigurableApplicationContext applicationContext;


    @PostConstruct
    public void printActiveProfiles() {
        log.debug("Application starting with active profiles: {}", Arrays.toString(environment.getActiveProfiles()));
    }

    public void restart() {
        log.debug("Restarting Application ...");
        applicationContext.refresh();
    }

}

