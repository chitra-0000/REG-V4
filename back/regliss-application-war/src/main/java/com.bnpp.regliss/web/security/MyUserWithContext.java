package com.bnpp.regliss.web.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.Arrays;

@SuppressWarnings("serial")
public class MyUserWithContext<U extends Serializable> extends org.springframework.security.core.userdetails.User {

    private final U user;
    private final AuthnContext authnContext;

    public MyUserWithContext(String username, U user, String role, AuthnContext authnContext) {
        super(username, "", Arrays.asList(new SimpleGrantedAuthority(role)));
        this.user = user;
        this.authnContext = authnContext;
    }

    public U getUser() {
        return user;
    }

    public AuthnContext getAuthnContext() {
        return authnContext;
    }

}

