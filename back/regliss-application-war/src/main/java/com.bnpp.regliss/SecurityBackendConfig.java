package com.bnpp.regliss;

import com.bnpp.regliss.web.security.JwtAuthenticationHeaderFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import static java.util.Arrays.asList;

@Configuration
@EnableWebSecurity//(debug = true)
@ConditionalOnExpression("#{environment.acceptsProfiles('server') && environment.acceptsProfiles('!bypassSecurity')}")
@Slf4j
public class SecurityBackendConfig extends WebSecurityConfigurerAdapter {
    @Value("${allowed.unsecured.endpoints}")
    private String[] allowedUnsecuredEndpoints;
    /**Implementation depends on external api or not*/
    @Autowired
    private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService;

    @Value("${jwt.backend.secret}")
    private String jwtSecret;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("Using Security: Real");
        log.warn("Allowing endpoints to pass without authentication: {}", asList(allowedUnsecuredEndpoints));
        http.authorizeRequests()
                .mvcMatchers(allowedUnsecuredEndpoints).permitAll()
                .mvcMatchers("/api/external/**").hasAnyRole("USER", "EXTERNAL")
                .mvcMatchers("/**").hasRole("USER")
                .and()
            //this is enabled in front java, should we verify csrf in back java also? how do we sync csrf between back and front java?
                .csrf().disable()
                .addFilter(jwtAuthorizationHeaderFilter())
                .headers().frameOptions().deny().contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-eval' 'unsafe-inline';" +
                        " style-src 'self' 'unsafe-inline'; style-src-elem 'self' 'unsafe-inline'; frame-ancestors 'self';")
                .and()
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.ORIGIN)
                .and()
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "accelerometer=(), camera=(), display-capture=(self), document-domain=()," +
                        " fullscreen=*, geolocation=(), microphone=(), payment=(), publickey-credentials-get=(self), sync-xhr=(self)"))
        ;
    }

    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider(AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService) {
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(userDetailsService);
        return authenticationProvider;
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(preAuthenticatedAuthenticationProvider(userDetailsService));
    }

    @Bean
    public JwtAuthenticationHeaderFilter jwtAuthorizationHeaderFilter() throws Exception {
        return new JwtAuthenticationHeaderFilter(authenticationManager(), jwtSecret);
    }
}

