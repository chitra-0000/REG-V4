package com.bnpp.regliss;

import com.bnpp.regliss.web.security.DummyDirectFormLoginAuthenticationProvider;
import com.bnpp.regliss.web.security.DummyQueryParamAuthenticationFilter;
import com.bnpp.regliss.web.security.JwtAuthenticationHeaderFilter;
import com.bnpp.regliss.web.security.StaticResourcesServedByBackendFilter;
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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import static java.util.Arrays.asList;

@Configuration
@EnableWebSecurity//(debug = true)
@ConditionalOnExpression("#{environment.acceptsProfiles('server') && environment.acceptsProfiles('bypassSecurity')}")
@Slf4j
public class SecurityBackendDummyConfig extends WebSecurityConfigurerAdapter {
    private static final String[] SWAGGER_RESOURCES = {
        "/swagger-ui.html", "/v2/api-docs", "/configuration/ui",
        "/swagger-resources/**", "/configuration/security", "/webjars/**"
    };

    /**
     * Implementation depends on external api or not
     */
    @Autowired
    private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService;

    @Value("${allowed.unsecured.endpoints}")
    private String[] allowedUnsecuredEndpoints;

    @Value("${jwt.backend.secret}")
    private String jwtSecret;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("Using Security: Dummy");
        log.warn("Allowing endpoints to pass without authentication: {}", asList(allowedUnsecuredEndpoints));
        http.authorizeRequests()
            .mvcMatchers(allowedUnsecuredEndpoints).permitAll()
            .mvcMatchers("/app/index.html").hasAnyRole("USER", "EXTERNAL")
            .mvcMatchers("/dummy-login", "/app/**").permitAll()
            .mvcMatchers(SWAGGER_RESOURCES).permitAll()
            .mvcMatchers("/api/external/**").hasAnyRole("USER", "EXTERNAL")
            .mvcMatchers("/**").hasRole("USER")
            .and().formLogin().loginPage("/dummy-login")
            .and()
            .csrf().disable()
            .addFilter(jwtAuthorizationHeaderFilter())
            .addFilterAfter(dummyQueryParamAuthenticationFilter(), BasicAuthenticationFilter.class)
            .headers().frameOptions().deny().contentSecurityPolicy("default-src 'self';" +
                        " script-src 'self' 'unsafe-eval' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; style-src-elem 'self' 'unsafe-inline'; frame-ancestors 'self';")
            .and()
            .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.ORIGIN)
            .and()
            .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "accelerometer=()," +
                    " camera=(), display-capture=(self), document-domain=(), fullscreen=*, geolocation=(), microphone=(), payment=(), publickey-credentials-get=(self), sync-xhr=(self)"))
        ;

    }

    @Bean
    public DummyQueryParamAuthenticationFilter dummyQueryParamAuthenticationFilter() throws Exception {
        return new DummyQueryParamAuthenticationFilter(authenticationManager());
    }

    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(userDetailsService);
        return authenticationProvider;
    }

    @Bean
    public DummyDirectFormLoginAuthenticationProvider dummyDirectFormLoginAuthenticationProvider() {
        return new DummyDirectFormLoginAuthenticationProvider(userDetailsService);
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(dummyDirectFormLoginAuthenticationProvider())
            .authenticationProvider(preAuthenticatedAuthenticationProvider());
    }

    @Bean
    public JwtAuthenticationHeaderFilter jwtAuthorizationHeaderFilter() throws Exception {
        return new JwtAuthenticationHeaderFilter(authenticationManager(), jwtSecret);
    }

    @Bean
    public StaticResourcesServedByBackendFilter servedByBackendFilter() {
        return new StaticResourcesServedByBackendFilter();
    }
}

