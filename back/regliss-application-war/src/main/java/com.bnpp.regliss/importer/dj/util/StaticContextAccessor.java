package com.bnpp.regliss.importer.dj.util;

// [MISSING FROM SCREENSHOTS: lines 3-7 of the original file were not visible in the photos]

@Component
public class StaticContextAccessor {

    private static StaticContextAccessor instance;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void registerInstance() { instance = this;    //NOSONAR }

    public static <T> T getBean(Class<T> clazz) { return instance.applicationContext.getBean(clazz); }

    public static StaticContextAccessor getInstance() { return instance; }

    public void setApplicationContext__FromTests(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}
