package com.bnpp.regliss.facade.dto.record;

import com.bnpp.regliss.entity.referential.record.RecordType;

public class RecordDto {
    public String id; // UUID in fact
    public RecordType type;

    public RecordCommonDto generalFields = new RecordCommonDto();
    public CityRecordDto cityFields;
    public CountryRecordDto countryFields;
    public AirportRecordDto airportFields;
    public OtherRecordDto otherFields;
    public PersonRecordDto personFields;
    public EntityRecordDto entityFields;
    public VesselRecordDto vesselFields;
    public SeaportRecordDto seaportFields;
    public Boolean hasWritePermissionOnList;
    public Boolean jasperPdfDownload;

    public RecordDto() {
    }

    private RecordDto(String id, RecordCommonDto common, RecordType recordType) {
        this.id = id;
        this.generalFields = common;
        this.type = recordType;
    }

    public RecordDto(String id, RecordCommonDto common, CityRecordDto city) {
        this(id, common, RecordType.CITY);
        this.cityFields = city;
    }

    public RecordDto(String id, RecordCommonDto common, CountryRecordDto country) {
        this(id, common, RecordType.COUNTRY);
        this.countryFields = country;
    }

    public RecordDto(String id, RecordCommonDto common, AirportRecordDto airport) {
        this(id, common, RecordType.AIRPORT);
        this.airportFields = airport;
    }

    public RecordDto(String id, RecordCommonDto common, OtherRecordDto other) {
        this(id, common, RecordType.OTHER);
        this.otherFields = other;
    }

    public RecordDto(String id, RecordCommonDto common, PersonRecordDto person) {
        this(id, common, RecordType.PERSON);
        this.personFields = person;
    }

    public RecordDto(String id, RecordCommonDto common, EntityRecordDto entity) {
        this(id, common, RecordType.ENTITY);
        this.entityFields = entity;
    }

    public RecordDto(String id, RecordCommonDto common, VesselRecordDto vessel) {
        this(id, common, RecordType.VESSEL);
        this.vesselFields = vessel;
    }

    public RecordDto(String id, RecordCommonDto common, SeaportRecordDto seaport) {
        this(id, common, RecordType.SEAPORT);
        this.seaportFields = seaport;
    }

}

