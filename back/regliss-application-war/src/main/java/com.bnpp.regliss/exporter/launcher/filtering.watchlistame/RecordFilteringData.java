package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.vo.RecordIdType;
import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class RecordFilteringData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long recordId;
    private final RecordType type;
    private final long createdInVersion;
    private final long clonedInVersion;
    private int[] categories;
    private int[] rcaIds;
    private int[] sanctionTypes;
    private int[] descriptions3;
    private int[] pepOccupationCategories;
    private int[] residenceIds;
    private int[] jurisdictionIds;
    private String[] deathDates;
    private String[] pepInactivityDates;
    private String[] rcaInactivityDates;
    private int[] genderTypes;

    public RecordFilteringData(long recordId, RecordType type, long createdInVersion, long clonedInVersion,int[] rcaIds) {
        this.recordId = recordId;
        this.type = type;
        this.createdInVersion=createdInVersion;
        this.clonedInVersion=clonedInVersion;
        this.rcaIds=rcaIds;

    }

    public RecordDescriptionIds getDescriptionIds() {
        return new RecordDescriptionIds(categories, sanctionTypes, descriptions3, pepOccupationCategories,genderTypes);
    }

    public RecordIdType getRecordIdType() { return new RecordIdType(recordId, type,createdInVersion,clonedInVersion); }

    public void setCategories(int[] categories) { this.categories = categories; }

    public void setSanctionTypes(int[] sanctionTypes) { this.sanctionTypes = sanctionTypes; }

    public void setPepOccupationCategories(int[] pepOccupationCategories) {
        this.pepOccupationCategories = pepOccupationCategories;
    }

    public void setDescriptions3(int[] descriptions3) { this.descriptions3 = descriptions3; }

    public int[] getResidenceIds() {
        return residenceIds != null ? residenceIds : new int[0]; // avoid instantiating millions of empty arrays
    }

    public void setResidenceIds(int[] residenceIds) { this.residenceIds = residenceIds; }

    public int[] getJurisdictionIds() { return jurisdictionIds != null ? jurisdictionIds : new int[0]; }

    public void setJurisdictionIds(int[] jurisdictionIds) { this.jurisdictionIds = jurisdictionIds; }

    public String[] getDeathDates() { return deathDates != null ? deathDates: new String[0]; }

    public void setDeathDates(String[] deathDates) { this.deathDates = deathDates; }

    public String[] getPepInactivityDates() { return pepInactivityDates != null ? pepInactivityDates : new String[0]; }

    public void setPepInactivityDates(String[] pepInactivityDates) { this.pepInactivityDates = pepInactivityDates; }

    public String[] getRcaInactivityDates() { return rcaInactivityDates != null ? rcaInactivityDates : new String[0]; }

    public void setRcaInactivityDates(String[] rcaInactivityDates) { this.rcaInactivityDates = rcaInactivityDates; }

    public void setGenderTypes(int[] genderTypes) { this.genderTypes = genderTypes; }
    public int[] getGenderTypes() {
        return genderTypes != null ? genderTypes : new int[0]; // avoid instantiating millions of empty arrays
    }
    public void setRcaIds(int[] rcaIds) { this.rcaIds=rcaIds; }
    public int[] getRcaIds() { return this.rcaIds; }

    public boolean isOnlyRca(){
        return rcaIds.length == categories.length && Arrays.asList(rcaIds).containsAll(Arrays.asList(categories));
    }
    public boolean hasRca(){
        List<Integer> categoryIdsList = Arrays.stream(categories).boxed().collect(toList());
        List<Integer> commonCategoryId = Arrays.stream(rcaIds).boxed().collect(toList());
        commonCategoryId.retainAll(categoryIdsList);
        return CollectionUtils.isNotEmpty(commonCategoryId);
    }

}
