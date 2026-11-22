package com.bnpp.regliss.statistics;

import lombok.Getter;

@Getter
public class VersionPersonStatistics {
    private long createdPersons;
    private long updatedPersons;
    private long deletedPersons;

    public VersionPersonStatistics(long createdPersons, long updatedPersons, long deletedPersons) {
        this.createdPersons = createdPersons;
        this.updatedPersons = updatedPersons;
        this.deletedPersons = deletedPersons;
    }
}
