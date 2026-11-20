package com.bnpp.regliss.profile;

import org.springframework.context.annotation.Profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Profile(ReglissProfile.REGLISS_EXTERNAL_API)
public @interface ReglissExternalAPIProfile {

}

