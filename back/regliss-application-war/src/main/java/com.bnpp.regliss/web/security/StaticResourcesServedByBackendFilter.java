package com.bnpp.regliss.web.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
/**
 * Marks static resources(html, js, css) served by backend
 * for frontend to determine the correct rest api prefix
 */
public class StaticResourcesServedByBackendFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(StaticResourcesServedByBackendFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
        //intentionally left empty
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        log.debug("Sending backend-server cookie");
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("backend-server", "true");
        httpResponse.addCookie(new Cookie("backend-server", "true"));
        chain.doFilter(request, response);
    }


    @Override
    public void destroy() {
        //intentionally left empty
    }

}

