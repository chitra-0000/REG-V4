package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.ReglissEnvironment;
import com.bnpp.regliss.entity.ServerEventsChannelType;
import com.bnpp.regliss.facade.ReferenceFacade;
import com.bnpp.regliss.facade.dto.ConfigDto;
import com.bnpp.regliss.facade.dto.reference.ReferentialType;
import com.bnpp.regliss.importer.manual.validation.MappingFileValidator;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.service.ServerEventsGateway;
import com.bnpp.regliss.web.boot.ApplicationRestarter;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bnpp.regliss.entity.ServerEventsChannelType.ADD_MOD_SUP_GENERATION_FINISHED;
import static com.bnpp.regliss.entity.ServerEventsChannelType.GENERATION_OR_DELIVERY_FINISHED;
import static com.bnpp.regliss.entity.ServerEventsChannelType.ADD_MOD_SUP_AUDIT_TRAIL_GENERATION_FINISHED;

@RestController
@ReglissWebProfile
public class TechnicalController {

    private static final Logger log = LoggerFactory.getLogger(TechnicalController.class);

    @Value("${application.version}")
    private String applicationVersion;

    @Value("${authentication.high.index.html.url}")
    private String highLoginIndexHtmlUrl;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private ApplicationRestarter applicationRestarter;

    @Autowired
    private ReferenceFacade facade;

    @Autowired
    private ServerEventsGateway serverPush;

    @Value("${url.vigilance}")
    private String vigilance;

    @Value("${url.echonet}")
    private String echonet;

    @Value("${url.help}")
    private String help;

    @Value("${url.high.authentication}")
    private String highAuthUrl;

    @Autowired
    private Environment environment;

    @GetMapping ("api/header/url")
    public Map<String, Object> getHeaderUrls()  {
        Map<String, Object> urls = new HashMap<>();
        urls.put("vigilance", vigilance);
        urls.put("echonet", echonet);
        urls.put("help", help);
        urls.put("highAuthUrl", highAuthUrl);
        return urls;
    }

    @GetMapping("api/reference")
    public Map<ReferentialType, List<Object>> getAllReferenceData() {
        return facade.retrieveUITranslationReferential();
    }

    @GetMapping("api/list-and-record-referential")
    public Map<ReferentialType, List<Object>> getListAndRecordReferential() {
        return facade.retrieveListAndRecordReferential();
    }


    @GetMapping("api/config")
    public ConfigDto getConfig() {
        ConfigDto dto = new ConfigDto();
        dto.isLocalDev = ReglissEnvironment.isLocalDev();
        dto.applicationVersion = applicationVersion;
        dto.highLoginIndexHtmlUrl = highLoginIndexHtmlUrl;
        dto.recordValidationEnabled = environment.acceptsProfiles(MappingFileValidator.STRONG_PROFILE);
        return dto;
    }

    @GetMapping("/api/serverEvents")
    public DeferredResult<Map<ServerEventsChannelType, String>> pollForEvents(
            @RequestParam("generationOrDeliveryFinished") String generationDeliveryToken,
            @RequestParam("addModSupGenerationFinished") String addModSupToken,
            @RequestParam("addModSupAuditTrailGenerationFinished") String addModSupAuditTrailToken
    ) {
        DeferredResult<Map<ServerEventsChannelType, String>> deferredResult = new DeferredResult<>();
        Map<ServerEventsChannelType, String> lastKnownTokenMap = ImmutableMap.of(
                GENERATION_OR_DELIVERY_FINISHED, generationDeliveryToken,
                ADD_MOD_SUP_GENERATION_FINISHED, addModSupToken,
                ADD_MOD_SUP_AUDIT_TRAIL_GENERATION_FINISHED, addModSupAuditTrailToken
        );

        serverPush.executeAtNextEvent(lastKnownTokenMap, deferredResult::setResult);

        return deferredResult;
    }

    @GetMapping("/restart")
    public String reloadBackendSpring() {
        log.debug("Reloading Application ...");
        applicationRestarter.restart();
        applicationContext.refresh();
        return "Reloading Application now " + LocalDateTime.now();
    }

    @GetMapping("/")
    public ModelAndView redirectToHome(HttpServletRequest request) {
        String redirectPath = "/app/index.html"; // NOSONAR: Prefixing with redirect: lets spring to handle the variable context-path
        if (request.getQueryString() != null) {
            redirectPath += "?" + request.getQueryString();
        }
        log.debug("Redirecting with Spring to {}", redirectPath);
        return new ModelAndView("redirect:" + UriComponentsBuilder.fromUriString(redirectPath).toUriString());
    }

}

