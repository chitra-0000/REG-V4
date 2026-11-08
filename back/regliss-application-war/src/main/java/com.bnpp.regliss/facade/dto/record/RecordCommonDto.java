package com.bnpp.regliss.facade.dto.record;

import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.facade.dto.BreadcrumbDto;

import java.util.ArrayList;
import java.util.List;

public class RecordCommonDto {
    public String name;
    public String description;
    public String keywords;
    public String notes;
    public String externalReference;
    public String reglissReference;
    public String deleteDate;
    public String startDate;
    public String comments;
    public String additionalInfo;
    public String instruction;
    public String recordId;
    public List<Long> sanctionTypes;
    public List<Long> descriptions;

    public List<ProgramDto> programs = new ArrayList<>();
    public List<AliasDto> aliases = new ArrayList<>();
    public List<SanctionReferenceDto> sanctionReferences = new ArrayList<>();
    public List<RecordSourceDto> sources = new ArrayList<>();
    public List<RecordRelationDto> relations = new ArrayList<>();
    public List<Long> programsIds = new ArrayList<>();
    public List<Long> categories = new ArrayList<>();

    public BreadcrumbDto breadcrumbDto = new BreadcrumbDto();

    public Confidentiality listConfidentiality;

    public ImportFileType importFileType;

    public Long idVersion;
    public boolean readOnly;
    public boolean isVersionLocked;
    public String lockingUser;
    public String synthesisModDate;
}

