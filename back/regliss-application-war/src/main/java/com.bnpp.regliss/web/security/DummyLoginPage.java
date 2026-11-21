package com.bnpp.regliss.web.security;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("bypassSecurity")
@RestController
public class DummyLoginPage {
    @GetMapping(path = "/dummy-login", produces = "text/html")
    public String getDummyLoginPage() {
        return "<html><head><title>Dummy Login Page</title></head><body>"
                + "<h1>Dummy Login</h1>"
                + "<h3>Please enter a valid login username</h3>"
                + "<form id='f' action='.'>"
                + "<input id='un' type='text' name='user' placeholder='username' autofocus/>"
                + "<select id='level' name='level'><option value='HIGH'>HIGH</option><option value='LOW'>LOW</option></select>"
                + "<input type='submit' value='Go'/><br/>"
                + "</form>"
                + "<input type='checkbox' name='rem' checked onclick='document.getElementById(\"f\").method = (this.checked ? \"GET\" : \"POST\");'/><label for='rem'>Remember Me</label>"
                + "<script>"
                + "function getQueryParam(name) { if(name=(new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(location.search)) return decodeURIComponent(name[1]);}"
                + "if (getQueryParam('user')) {"
                + "document.getElementById('un').value=getQueryParam('user');"
                + "document.getElementById('level').value=getQueryParam('level');"
                + "document.getElementById(\"f\").submit();"
                + "}"
                + "</script>"
                + "</body></html>";
    }
}
