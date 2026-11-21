package com.bnpp.regliss.web.security;

import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.repository.UserRepository;
import com.bnpp.regliss.service.ReglissRequestContext;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import java.security.SecureRandom;
import java.time.LocalDateTime;

public class RequestContextExtractor implements WebRequestInterceptor {

    private static final int REQUEST_ID_LENGTH = 4;

    private static final Logger log = LoggerFactory.getLogger(RequestContextExtractor.class);

    @Autowired
    private ReglissRequestContext requestContext;

    @Override
    @SuppressWarnings("unchecked")
    public void preHandle(WebRequest webRequest) throws Exception {
        SecureRandom secRandom = new SecureRandom();
        byte[] result = new byte[2*REQUEST_ID_LENGTH];
        secRandom.nextBytes(result);
        requestContext.setRequestId(Hex.encodeHexString(result));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            MyUserWithContext<User> securityUser = (MyUserWithContext<User>) auth.getPrincipal();
            User user = securityUser.getUser();
            log.trace("Changing request context in request: {}", webRequest.getDescription(false));
            requestContext.setCurrentUser(user);
            requestContext.setHighAuthentication(securityUser.getAuthnContext() == AuthnContext.HIGH);
            requestContext.setRequestTime(LocalDateTime.now());

            if (!"true".equals(webRequest.getHeader("Polling"))) {
                log.debug("User {} ({}/{}) started request {}", requestContext.getUsername(), requestContext.getLanguage(), requestContext.isHighAuthentication(), requestContext.getRequestId());
            }
        }
    }

    @Autowired
    private UserRepository userRepo;

    @Override
    public void afterCompletion(WebRequest arg0, Exception arg1) throws Exception {
        //intentionally left empty
    }

    @Override
    public void postHandle(WebRequest arg0, ModelMap arg1) throws Exception {
        //intentionally left empty
    }

}

