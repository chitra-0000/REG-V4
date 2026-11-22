package com.bnpp.regliss.search;

import com.bnpp.regliss.entity.ListVersionDTO;

import java.util.List;

public class VigilanceRecordSearchCriteria {
    private static final boolean PART_OF_RECORD=true;
    public enum VigilanceSearchCodeType {
        IMO(PART_OF_RECORD), REGLISS_REFERENCE(PART_OF_RECORD),
        EXTERNAL_REFERENCE(PART_OF_RECORD), RECORD_ID(PART_OF_RECORD),
        ISIN, BIC, IBAN, CUSIP;

        private final boolean partOfRecord;
        VigilanceSearchCodeType(boolean isPartOfRecord) { partOfRecord=isPartOfRecord; }
        VigilanceSearchCodeType() { partOfRecord=false; }
        public boolean isPartOfRecord() { return partOfRecord; }
    }
    public VigilanceSearchCodeType codeType;
    public String codeValue;
    public List<ListVersionDTO> searchedVersions;
}
