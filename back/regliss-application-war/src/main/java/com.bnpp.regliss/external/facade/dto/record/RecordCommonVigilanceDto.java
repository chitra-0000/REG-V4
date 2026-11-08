package com.bnpp.regliss.external.facade.dto.record;

import java.util.ArrayList;
import java.util.List;


public class RecordCommonVigilanceDto {

    public ListDto list;
    public String name;
    public String description;
    public String keyword;  
    public String notes;
    public String externalReference;
    public String reglissReference;
    public String deleteDate;
    public String applicationDate;

    public String remarks;
    public String additionalInfo;
    public String instruction;
    public String recordId;
    public List<String> sanctionTypesLabel;

    public List<ProgramVigilanceDto> programs = new ArrayList<>();
    public List<AliasVigilanceDto> aliases = new ArrayList<>();
    public List<SanctionReferenceVigilanceDto> sanctionReferences =  new ArrayList<>();
    public List<RecordSourceVigilanceDto> sources =  new ArrayList<>();
    public List<RecordRelationVigilanceDto> relations =  new ArrayList<>();
    public List<String> categoriesLabel = new ArrayList<>();

}

