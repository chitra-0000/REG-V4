package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-10 of the original file were not visible in the photos]

public class ReglissListDto {

    public Long id;
    public String reference;
    public String shortName;
    public String name;
    public String description;
    public Long regulatorOrInternalPolicy;
    public ReglissListType type;
    public Confidentiality confidentiality;
    public Scope scope;
    public Long country;
    public Long territory;
    public String owner;
    public Long issuer;
    public Long listCategory;
    public boolean active;
    @Getter
    public ListVersionDto applicableVersion;
    @Getter
    public ListVersionDto currentVersion;
    public String commentFr;
    public String commentEn;
    public List<BroadcastEmailDto> broadcastEmails;
    public boolean canDeactivate;
    public boolean canDelete;
    public boolean isGlobalPlusOfac;
    public boolean genericFormat;
    public boolean reglissFormat;
    public boolean vigilanceFormat;
    public boolean safewatchFormat;
    public boolean fircosoftFormat;
    public boolean djCoreXmlFormat;
    public List<ListVersionDto> versions;
    public MappingFileDto mappingFile;
    public ImportConfigurationDto importConfiguration;
    public List<DJExportFilterAndSubfiltersDto> filters;
    public List<SixFiltersAndSubFiltersDto> sixFilters;
    public BreadcrumbDto breadcrumbDto;
    public Long geographicData;
    public boolean isReadOnly;
    public boolean isVersionReadOnly;
    public boolean isLastVersionPublishedOrDelivered;

    public ReglissListDto() {
    }
    public ReglissListDto(Long id) { this.id = id; }

}

