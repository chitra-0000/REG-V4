package com.bnpp.regliss.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ConsultedEntitiesInSession {

    private Set<Long> consultedListIds = new HashSet<>();
    private Set<Long> consultedVersionIds = new HashSet<>();
    private Set<Long> consultedRecordIds = new HashSet<>();

    /**
     * @param listId
     * @return <code>true</code> if the id was new.
     */
    public boolean addConsultedList(Long listId) { return consultedListIds.add(listId); }

    public boolean addConsultedVersion(Long versionId) { return consultedVersionIds.add(versionId); }

    public boolean addConsultedRecord(Long recordId) { return consultedRecordIds.add(recordId); }

}
