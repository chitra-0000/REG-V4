package com.bnpp.regliss.web.security;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Arrays;

public class DummyDirectFormLoginAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(DummyDirectFormLoginAuthenticationProvider.class);

    private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> authenticationUserDetailsService;

    public DummyDirectFormLoginAuthenticationProvider(AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.equals(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (StringUtils.isBlank(authentication.getName())) {
            return null;
        }

        PreAuthenticatedAuthenticationToken userWithContext = new PreAuthenticatedAuthenticationToken(new UsernameContextPrincipal(authentication.getName(), "ROLE_USER", AuthnContext.HIGH),
                "N/A");

        UserDetails ud = authenticationUserDetailsService.loadUserDetails(userWithContext);

        log.debug("Blindly allowing the request to get in with ROLE_USER");
        return new PreAuthenticatedAuthenticationToken(ud, "", Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}

