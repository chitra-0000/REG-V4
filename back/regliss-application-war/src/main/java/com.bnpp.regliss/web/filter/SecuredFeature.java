package com.bnpp.regliss.web.filter;

import com.bnpp.regliss.vo.FeaturePermission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SecuredFeature {
    FeaturePermission[] value();
}

