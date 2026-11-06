package com.bnpp.regliss.batch.service;

import com.bnpp.regliss.entity.CftType;
import com.bnpp.regliss.profile.ReglissProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class NodeDetailsSupplier {
    @Autowired
    private Environment env;

    @PostConstruct
    public void logNodeDetails() { log.info("node properties nodeId:{}, hostName:{} ", getNodeId(), getHostName()); }

    public String getNodeId() { return System.getProperty("node.name", "NA"); }

    public String getHostName() { return System.getProperty("host.name", "NA"); }
    public Optional<Integer> getHttpPort() {
        String port = System.getProperty("http_port", null);
        return port == null ? Optional.empty() : Optional.of(Integer.parseInt(port));
    }

    public CftType getCftType() {
        String cftTypeString = System.getProperty("cft.type", "NA");
        return CftType.fromString(cftTypeString);
    }

    public boolean isPrimaryReglissPublisher() {
        return Arrays.asList(env.getActiveProfiles()).contains(ReglissProfile.REGLISS_PUBLISHER) && getCftType().isPrimary();
    }
    public boolean isReglissPublisher() {
        return Arrays.asList(env.getActiveProfiles()).contains(ReglissProfile.REGLISS_PUBLISHER);
    }

    public List<String> getActiveProfiles() { return Arrays.stream(env.getActiveProfiles()).collect(toList()); }
}
