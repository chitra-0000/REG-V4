package com.bnpp.regliss.facade.dto.version;

// [MISSING FROM SCREENSHOTS: lines 3-7 of the original file were not visible in the photos]

public class RecordInVersionSearchResultDto {

    public String id;
    public String name;
    public Set<Long> categories = new HashSet<>();
    public RecordType type;
    public String reference;
    public String code;
    public String program;
    public Confidentiality listConfidentiality;

}

