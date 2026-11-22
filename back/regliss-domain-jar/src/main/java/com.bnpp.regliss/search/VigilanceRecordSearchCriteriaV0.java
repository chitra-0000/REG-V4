package com.bnpp.regliss.search;

import com.bnpp.regliss.entity.Version;

import java.util.List;

public class VigilanceRecordSearchCriteriaV0 {
    private static final boolean PART_OF_RECORD=true;

    public VigilanceRecordSearchCriteria.VigilanceSearchCodeType codeType;
    public String codeValue;
    public List<Version> searchedVersions;
}
