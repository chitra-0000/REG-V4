package com.bnpp.regliss;

import com.bnpp.regliss.helper.TempFileUtils;
import com.bnpp.regliss.profile.ReglissProfile;
import com.bnpp.regliss.repository.base.EntityRepositoryFactoryBean;
import com.bnpp.regliss.utils.thread.ThreadScope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@Slf4j
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableCaching
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.bnpp.regliss.repository", repositoryFactoryBeanClass = EntityRepositoryFactoryBean.class)
public class ReglissApplication extends SpringBootServletInitializer implements ApplicationListener<WebServerInitializedEvent> {

    private static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
    private static final String STATIC_RESOURCES_LOCATIONS = "spring.resources.static-locations";
    private static final String HOST_NAME = "host.name";
    private static final String NODE_NAME = "node.name";

    @Value("${application.version}")
    private String applicationVersion;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private TempFileUtils tempFileUtils;

    @PostConstruct
    public void hello() {
        log.warn("STARTING with profiles: " + Arrays.toString(context.getEnvironment().getActiveProfiles()));
        log.warn("Regliss application version :"+applicationVersion);
        tempFileUtils.recreateTempFolderForCurrentNode();
        if (ReglissEnvironment.isLocalDev()) {
            log.warn("Running on local environment");
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        /*main will be called only when running as Spring Boot Application on a local environment
         * We use main to set default properties used ONLY in a local environment*/
        System.setProperty("regliss.env", "local");

        if(System.getProperty(HOST_NAME)==null) {
            System.setProperty(HOST_NAME, java.net.InetAddress.getLocalHost().getHostName());
        }
        log.info("Using host name {}, and node name {} ", System.getProperty(HOST_NAME), System.getProperty(NODE_NAME));
        if(System.getProperty(STATIC_RESOURCES_LOCATIONS) == null) {
            System.setProperty(STATIC_RESOURCES_LOCATIONS, new File(".").toURI() + "regliss-front-war/src/main/resources/public/");
        }
        log.info("Using web static resources from {}", System.getProperty(STATIC_RESOURCES_LOCATIONS));

        new SpringApplicationBuilder(ReglissApplication.class)
                .run(args);
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        addProfilesFromJvmArguments(servletContext);
        super.onStartup(servletContext);
    }

    @Bean
    public static BeanFactoryPostProcessor registerThreadScope() {
        return beanFactory -> beanFactory.registerScope("thread", new ThreadScope());
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        return messageSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new JpaTransactionManager(emf);
    }

    private void addProfilesFromJvmArguments(ServletContext servletContext) {
        if (servletContext.getInitParameter(SPRING_PROFILES_ACTIVE) != null) {
            throw new IllegalArgumentException("You should not set " + SPRING_PROFILES_ACTIVE + " in web.xml while using this class. Instead, add them in code of this class.");
        }

        String newProfiles = "server";

        String jvmProfiles = System.getProperty(SPRING_PROFILES_ACTIVE);
        if (jvmProfiles != null) {
            newProfiles += "," + jvmProfiles;
        }
        if(servletContext.getContextPath().replace("/", "").equals(loadElementDetailContext())){
            newProfiles=prepareExternalAppProfiles(newProfiles);
        }
        log.info("Setting Spring active profiles to: {}" , newProfiles);
        servletContext.setInitParameter(SPRING_PROFILES_ACTIVE, newProfiles);
    }

    private String loadElementDetailContext() {
        Properties props=new Properties();
        log.info("Checking if application will be started ");
        try {
            props.load(this.getClass().getResourceAsStream(PropertiesLocationResolver.resolveConfigPropertiesPathBasedOnEnvironment()));
            return props.getProperty("external.element.detail.context");
        }catch(Exception e) {
            log.error("Could not load properties to determine elementDetail context: {}",e.getMessage());
            return null;
        }
    }

    /**
     * Remove reglissBatch from active profiles and add reglissExternalAPI
     *
     * @param activeProfiles
     * @return
     */
    private String prepareExternalAppProfiles(String activeProfiles) {
        Set<String> restrictedprofiles = Stream.of(
                "bypassSecurity",
                ReglissProfile.REGLISS_BATCH,
                ReglissProfile.REGLISS_PUBLISHER,
                ReglissProfile.REGLISS_WEB
        ).collect(toSet());
        return Stream.concat(Stream.of(activeProfiles.split(","))
                        .filter(StringUtils::isNotEmpty)
                        .filter(profile -> !restrictedprofiles.contains(profile)),
                Stream.of(ReglissProfile.REGLISS_EXTERNAL_API))
                .collect(joining(","));
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
        int port = webServerInitializedEvent.getWebServer().getPort();
        if (System.getProperty("http_port") == null) {
            System.setProperty("http_port", Integer.toString(port));
        }
    }
}




