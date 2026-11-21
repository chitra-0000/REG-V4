package com.bnpp.regliss.web.security;

import java.io.Serializable;

public class UsernameContextPrincipal implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String appRole;
    private final AuthnContext context;

    public UsernameContextPrincipal(String username, String appRole, AuthnContext context) {
        this.username = username;
        this.context = context;
        this.appRole=appRole;
    }

    public String getUsername() {
        return username;
    }


    public String getAppRole() {
        return appRole;
    }

    public AuthnContext getContext() {
        return context;
    }



}

