package com.bnpp.regliss.web.filter;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@ControllerAdvice
public class DataBinderConfigurer {

    @InitBinder
    protected void initBinder(WebDataBinder binder) {

        for (Class<?> searchCriteriaClass : findSearchCriteriaClasses()) {
            binder.registerCustomEditor(searchCriteriaClass, new JacksonPropertyEditor(searchCriteriaClass));
        }
    }

    private static List<Class<?>> findSearchCriteriaClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

        try {
            List<Class<?>> list = new ArrayList<>();
            for (BeanDefinition bd : scanner.findCandidateComponents("com.bnpp.regliss.search")) {
                list.add(Class.forName(bd.getBeanClassName())); //NOSONAR
            }
            return list;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not scan the classpath", e);
        }
    }
}

