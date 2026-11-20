package com.bnpp.regliss.scheduler.sixcftgtw;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.config.dbprops.DatabaseConfigurationProvider;
import com.bnpp.regliss.entity.CftgtwRequest;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import com.bnpp.regliss.repository.CftgtwRequestRepository;
import com.bnpp.regliss.service.ReferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
@ReglissPublisherProfile
@Slf4j
public class SixCftgtwRequestScheduler {


    private static final String SIXLIST_PROPERTY = "automatic.import.cftgtw_request_hours.SIX";

    @Autowired
    private CftgtwRequestRepository cftgtwRequestRepository;

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private HeartbeatService heartbeatService;

    //@Scheduled(cron = "30 0/1 * * * ?")task.batch.export.generation
    @Scheduled(cron = "${task.create_sixcftgtw.cron}")
    public void scheduleSixCftgtwRequests() {
        if (heartbeatService.isNodeActivePublisher()) {
            createCftGtwRequestIfItsTime();
        }
    }

    private void createCftGtwRequestIfItsTime() {
        LocalTime currentMinute = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        Map<ImportFileType, List<LocalTime>> configuredHoursPerFormat = new EnumMap<>(ImportFileType.class);
        Properties configuredProperties = DatabaseConfigurationProvider.getResolvedProperties(new JdbcTemplate(dataSource));


        Stream.of(SIXLIST_PROPERTY).forEach(property -> configuredHoursPerFormat
                .put(getFormat(property), getConfiguredHoursForProperty(configuredProperties, property)));

        for ( Entry<ImportFileType, List<LocalTime>> entry : configuredHoursPerFormat.entrySet()) {
            ImportFileType format = entry.getKey();

            Optional<LocalTime> timeToSendRequestOpt = entry.getValue().stream()
                    .filter(currentMinute::equals)
                    .findFirst();
            timeToSendRequestOpt.ifPresent(requestTime -> createCftGtwRequest(format, requestTime));
        }
    }

    private void createCftGtwRequest(ImportFileType format, LocalTime timeToSendRequest) {
        LocalDateTime requestTime = LocalDateTime.now()
            .withHour(timeToSendRequest.getHour()).withMinute(timeToSendRequest.getMinute());
        log.debug("Persisting a six cftgtw request for configured hour: {} and format: {}  "
            , requestTime, format);
        cftgtwRequestRepository.save(new CftgtwRequest(format, requestTime, referenceService.getSysUser()));
    }

    private ImportFileType getFormat(String format) {
        if (format.equalsIgnoreCase(SIXLIST_PROPERTY)) {
            return ImportFileType.SIX_LIST;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private List<LocalTime> getConfiguredHoursForProperty(Properties configuredProperties, String format) {
        return Stream.of(configuredProperties.getProperty(format).split(","))
                .map(t -> LocalTime.parse(t, DateTimeFormatter.ofPattern("HH:mm")))
                .collect(toList());
    }
}

