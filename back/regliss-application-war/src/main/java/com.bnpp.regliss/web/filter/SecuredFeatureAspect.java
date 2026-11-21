package com.bnpp.regliss.web.filter;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.permission.CurrentFeaturePermissions;
import org.apache.commons.collections.CollectionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static java.util.Arrays.asList;

@ReglissWebProfile
@Component
@Aspect
public class SecuredFeatureAspect implements BeanNameAware{
    private static final Logger log = LoggerFactory.getLogger(SecuredFeatureAspect.class);

    @Autowired
    private CurrentFeaturePermissions currentFeaturePermissions;
    @Autowired
    private ReglissRequestContext requestContext;

    @Before("execution(* *(..)) && @annotation(annotation))")
    public void checkFeaturePermission(JoinPoint joinPoint, SecuredFeature annotation) throws Throwable {
        log.debug("Checking permission '{}' to access method '{}' by user: {} with profile: {}",
                annotation.value(),
                joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName(),
                requestContext.getUsername(),
                requestContext.getCurrentUser().getProfile());
        Collection<?> intersection = CollectionUtils.intersection(currentFeaturePermissions.getFeaturePermissions(), asList(annotation.value()));

        boolean hasAtLeastOneFeature = intersection.size() > 0;
        if (!hasAtLeastOneFeature) {
            log.error("User {} with profile {}, high auth={} has no access on feature {}",
                    requestContext.getUsername(),
                    requestContext.getCurrentUser().getProfile(),
                    requestContext.isHighAuthentication(),
                    annotation.value());
            throw new ReglissException(ErrorCode.NOT_ALLOWED_BY_PROFILE);
        }
    }

    @Override
    public void setBeanName(String beanName) {
        log.debug("Defining aspect in bean name: {}" , beanName);
    }
}

