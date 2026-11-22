package com.bnpp.regliss.vo;

import com.bnpp.regliss.utils.DJFilenameHelper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class DJFeedId extends AutomaticImportFeedIdBase {

    private final DJFileImportMode importMode;

    public DJFeedId(String prefix, String dateStamp, DJFileImportMode importMode) {
        super();
        super.prefix = prefix;
        super.orderingMarker = dateStamp;
        this.importMode = importMode;
    }

    @Override
    public String getDateStamp() {
        return super.orderingMarker;
    }

    @Override
    public Long getSourceVersion() {
        return null;
    }

    @Override
    public String getJuridictionBase() {
        // using the default value
        return null;
    }

    @Override
    public boolean isOlderThan(AutomaticImportFeedIdBase previousFeed) {
        DJFeedId previousDJFeed = (DJFeedId) previousFeed;
        if (previousFeed.getDateStamp().compareTo(getDateStamp()) > 0) {
            return true;
        }
        return previousFeed.getDateStamp().compareTo(this.getDateStamp()) == 0
                && (previousDJFeed.getImportMode() == DJFileImportMode.FULL
                || this.getImportMode() == DJFileImportMode.INCREMENTAL);
    }

    @Override
    public boolean isFull() {
        return importMode == DJFileImportMode.FULL;
    }

    public boolean isWatchlist() {
        return DJFilenameHelper.isWatchlistXml(prefix);
    }

    public Optional<LocalDate> getDateOpt() {
        try {
            Pattern p = Pattern.compile("([0-9]{8})(.*)");
            Matcher m = p.matcher(orderingMarker);
            if (!m.matches()) {
                throw new IllegalArgumentException(
                        String.format("The date in filename '%s' does not match expected pattern: %s", orderingMarker, p.pattern())
                );
            }
            String date = m.group(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            return Optional.of(LocalDate.parse(date, formatter));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Optional.empty();
        }
    }
}
