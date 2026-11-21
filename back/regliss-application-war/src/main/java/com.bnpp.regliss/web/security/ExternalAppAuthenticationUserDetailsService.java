package com.bnpp.regliss.web.security;

import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.profile.ReglissExternalAPIProfile;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ReglissExternalAPIProfile
public class ExternalAppAuthenticationUserDetailsService implements  AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(DatabaseOrRefogAuthenticationUserDetailsService.class);

    @Override
    @Transactional
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) {
        UsernameContextPrincipal principal = (UsernameContextPrincipal) token.getPrincipal();
        String username = principal.getUsername();
        if (StringUtils.isBlank(username)) {
            throw new UsernameNotFoundException("No username provided");
        }
        if(!"EXTERNAL".equals(principal.getAppRole())) {
            throw new UsernameNotFoundException("No username provided");
        }

        //for external users accessing only external element detail
        User user=new User();
        user.setUsername(principal.getUsername());
        log.debug("Allowing the request to get in as external app user");
        return new MyUserWithContext<>(principal.getUsername(), user, "ROLE_EXTERNAL", principal.getContext());
    }
}

