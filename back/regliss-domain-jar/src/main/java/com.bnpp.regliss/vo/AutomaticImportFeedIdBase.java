package com.bnpp.regliss.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Optional;

@Data
@EqualsAndHashCode
public abstract class AutomaticImportFeedIdBase implements Comparable<AutomaticImportFeedIdBase> {

    protected String prefix;
    protected String orderingMarker;

    public abstract String getDateStamp();

    public abstract Long getSourceVersion();

    public abstract String getJuridictionBase();

    public abstract boolean isOlderThan(AutomaticImportFeedIdBase previousFeed);

    public abstract Optional<LocalDate> getDateOpt();

    public abstract boolean isFull();

    @Override
    public int compareTo(AutomaticImportFeedIdBase other) {
        return orderingMarker.compareTo(other.orderingMarker);
    }

}
