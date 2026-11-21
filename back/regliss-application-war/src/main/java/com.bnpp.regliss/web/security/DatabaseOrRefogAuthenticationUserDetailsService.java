package com.bnpp.regliss.web.security;

import com.bnpp.regliss.ReglissEnvironment;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.profile.ReglissProfile;
import com.bnpp.regliss.repository.UserRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!" + ReglissProfile.REGLISS_EXTERNAL_API)
public class DatabaseOrRefogAuthenticationUserDetailsService implements  AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {


    private static final Logger log = LoggerFactory.getLogger(DatabaseOrRefogAuthenticationUserDetailsService.class);

    @Autowired
    @Lazy
    private UserRepository userRepository;

    @Value("${users.banned}")
    private String usersBanned; // test,moo,...

    @Value("${is.production.environment}")
    private boolean isProductionEnvironment;

    @Override
    @Transactional
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) {
        UsernameContextPrincipal principal = (UsernameContextPrincipal) token.getPrincipal();
        String username = principal.getUsername();
        if (StringUtils.isBlank(username)) {
            throw new UsernameNotFoundException("No username provided");
        }

        forbidMockUsers(username);

        log.debug("Lookup username {} in database", username);
        User user = userRepository.findByIdFetchListPermissions(username);

        if (user == null) {
            log.warn("Username {} not found in the USERS table. Forbidding acces.", username);
            throw new UsernameNotFoundException("Username " +username+ " not found in the database");
        } else {
            log.debug("User found in database");
            if (! user.isActive()) {
                throw new UsernameNotFoundException("User is inactive in the database");
            }

            if (isProfileForbidden(user.getProfile())){
                log.warn("User with profile not available on PROD environments. Forbidding acces.");
                throw new UsernameNotFoundException("User with profile not available on PROD environments");
            }
        }

        log.debug("Allowing the request to get in wirh ROLE_USER");
        return new MyUserWithContext<>(user.getUsername(), user, "ROLE_USER", principal.getContext());
    }


    private void forbidMockUsers(String username) {
        if (!ReglissEnvironment.isLocalDev() &&
            usersBanned.contains(username)) {
            throw new UsernameNotFoundException("User '"+username+"' was banned from the database");
        }
    }

    boolean isProfileForbidden(com.bnpp.regliss.entity.Profile profile) {
        return (isProductionEnvironment && profile == com.bnpp.regliss.entity.Profile.SUPER_PROFILE);
    }
}

