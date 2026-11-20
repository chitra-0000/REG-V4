package com.bnpp.regliss.scheduler.cftgtw;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.config.dbprops.DatabaseConfigurationProvider;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.CftgtwRequest;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
@ReglissPublisherProfile
@Slf4j
public class CftgtwRequestScheduler {

    private static final String WATCHLIST_PROPERTY = "automatic.import.cftgtw_request_hours.WATCHLIST";
    private static final String WATCHLIST_EXTRA_PROPERTY = "automatic.import.cftgtw_request_hours.WL_EXTRA";
    private static final String CORE_PROPERTY = "automatic.import.cftgtw_request_hours.CORE";
    private static final String AME_PROPERTY = "automatic.import.cftgtw_request_hours.AME";
    private static final String WATCHLIST_ALLOWED_DAYS = "automatic.import.cftgtw_request_days.WATCHLIST";

    @Autowired
    private CftgtwRequestRepository cftgtwRequestRepository;

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private HeartbeatService heartbeatService;

    @Scheduled(cron = "30 0/1 * * * ?")
    public void scheduleCftgtwRequests() {
        if (heartbeatService.isNodeActivePublisher()) {
            createCftGtwRequestIfItsTime();
        }
    }

    private void createCftGtwRequestIfItsTime() {
        LocalTime currentMinute = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        Map<ImportFileType, List<LocalTime>> configuredHoursPerFormat = new EnumMap<>(ImportFileType.class);
        Properties configuredProperties = DatabaseConfigurationProvider.getResolvedProperties(new JdbcTemplate(dataSource));
        int today = LocalDateTime.now().getDayOfMonth();
        int lastDayOfMonth = LocalDateTime.now().toLocalDate().lengthOfMonth();
        List<Integer> allowedDays = getWatchlistAllowedDays(configuredProperties);
        allowedDays.add(lastDayOfMonth);

        Stream.of(WATCHLIST_PROPERTY, WATCHLIST_EXTRA_PROPERTY, CORE_PROPERTY, AME_PROPERTY).forEach(property -> configuredHoursPerFormat
                .put(getDJFormat(property), getConfiguredHoursForProperty(configuredProperties, property)));

        for ( Entry<ImportFileType, List<LocalTime>> entry : configuredHoursPerFormat.entrySet()) {
            ImportFileType format = entry.getKey();
            if (format == ImportFileType.DOW_JONES_WATCHLIST && !allowedDays.contains(today)) {
                continue;
            }
            Optional<LocalTime> timeToSendRequestOpt = entry.getValue().stream()
                    .filter(currentMinute::equals)
                    .findFirst();
            timeToSendRequestOpt.ifPresent(requestTime -> createCftGtwRequest(format, requestTime));
        }
    }

    private void createCftGtwRequest(ImportFileType format, LocalTime timeToSendRequest) {
        LocalDateTime requestTime = LocalDateTime.now()
            .withHour(timeToSendRequest.getHour()).withMinute(timeToSendRequest.getMinute());
        log.debug("Persisting a cftgtw request for configured hour: {} and format: {}  "
            , requestTime, format);
        cftgtwRequestRepository.save(new CftgtwRequest(format, requestTime, referenceService.getSysUser()));
    }

    private ImportFileType getDJFormat(String djFormat){
        switch (djFormat) {
        case WATCHLIST_PROPERTY: return ImportFileType.DOW_JONES_WATCHLIST;
        case WATCHLIST_EXTRA_PROPERTY: return ImportFileType.DOW_JONES_WATCHLIST_EXTRA;
        case CORE_PROPERTY: return ImportFileType.DOW_JONES_CORE;
        case AME_PROPERTY: return ImportFileType.DOW_JONES_AME;
        default: throw new IllegalArgumentException();
        }
    }

    private List<LocalTime> getConfiguredHoursForProperty(Properties configuredProperties, String djFormat) {
        return Stream.of(configuredProperties.getProperty(djFormat).split(","))
                .map(t -> LocalTime.parse(t, DateTimeFormatter.ofPattern("HH:mm")))
                .collect(toList());
    }

    private List<Integer> getWatchlistAllowedDays(Properties prop){
        String days = prop.getProperty(WATCHLIST_ALLOWED_DAYS);
        if (days == null || days.trim().isEmpty()){
            return Collections.emptyList();
        }
        return Arrays.stream(days.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}

