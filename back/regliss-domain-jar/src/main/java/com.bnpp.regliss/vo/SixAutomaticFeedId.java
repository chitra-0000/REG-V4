package com.bnpp.regliss.vo;

import com.bnpp.regliss.ReglissException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class SixAutomaticFeedId extends AutomaticImportFeedIdBase {

    @Getter
    @Setter
    private Long sourceVersion;

    private LocalDate applicationDate;

    @Getter
    @Setter
    private String juridictionBase;

    public SixAutomaticFeedId(String prefix, String orderingMarker) {
        super();
        super.prefix = prefix;
        super.orderingMarker = orderingMarker;
    }

    public void setApplicationDate(String dateAsString) {
        if (StringUtils.isNotEmpty(dateAsString)) {
            this.applicationDate = LocalDate.parse(dateAsString);
        }
    }

    @Override
    public String getDateStamp() {
        LocalDate date = applicationDate == null ? LocalDate.now() : applicationDate;
        return DateTimeFormatter.BASIC_ISO_DATE.format(date);
    }

    @Override
    public Optional<LocalDate> getDateOpt() {
        return Optional.ofNullable(applicationDate);
    }

    @Override
    public boolean isOlderThan(AutomaticImportFeedIdBase previousFeed) {
        try {
            log.info("Previous ordermarker : {}, Current ordermarker : {}", previousFeed.getOrderingMarker(), this.getOrderingMarker());
            return Long.parseLong(previousFeed.getOrderingMarker()) >= Long.parseLong(this.getOrderingMarker());
        } catch (NumberFormatException e) {
            throw new ReglissException(e, e.getMessage());
        }
    }

    @Override
    public boolean isFull() {
        return true;
    }
}

