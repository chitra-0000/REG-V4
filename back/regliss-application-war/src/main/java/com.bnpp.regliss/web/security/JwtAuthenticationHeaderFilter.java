package com.bnpp.regliss.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
@Slf4j
public class JwtAuthenticationHeaderFilter extends AbstractPreAuthenticatedProcessingFilter {

    private static final String JWT_HEADER_NAME = "x-bnpp-jwt-1";

    private final String backendSecret;

    public JwtAuthenticationHeaderFilter(AuthenticationManager authenticationManager, String backendSecret) {
        setAuthenticationManager(authenticationManager);
        this.backendSecret = backendSecret;
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        log.debug("filter request {}", requestURL);
        String encodedJwt = request.getHeader(JWT_HEADER_NAME);
        if (encodedJwt == null) {
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(backendSecret))
                    .parseClaimsJws(encodedJwt)
                    .getBody();
            log.debug("Authenticating user {} with jwt", claims.getSubject());
            return new UsernameContextPrincipal(claims.getSubject(),getAuthRole(claims), getAuthnContext(claims));
        } catch (UnsupportedJwtException jwtException) {
            throw new PreAuthenticatedCredentialsNotFoundException("Invalid JWT Token", jwtException);
        }
    }

    private String getAuthRole(Claims claims) {
        log.debug("authenticating with role {}",claims.get("AppRole", String.class));
        return claims.get("AppRole", String.class);
    }

    private AuthnContext getAuthnContext(Claims claims) {
        String contextStr = claims.get("AuthnContext", String.class);
        if (contextStr == null) {
            return null;
        }
        return AuthnContext.valueOf(contextStr);
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "N/A";
    }

}

