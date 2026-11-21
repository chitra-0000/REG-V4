package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.profile.ReglissProfile;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;


/**
 * Component to simulate backend actuator health endpoint responding with 503 code
 * */
@RestController
@Profile({ReglissProfile.REGLISS_WEB, ReglissProfile.REGLISS_EXTERNAL_API})
public class HealthCheckController {

    @GetMapping(value = "actuator/health", produces = "application/json")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "UP");
    }
}

