package com.bnpp.regliss.external.facade.dto.record;

public class RecordVigilanceDto {

    public String type;

    public RecordCommonVigilanceDto generalFields = new RecordCommonVigilanceDto();
    public CityRecordVigilanceDto cityFields;
    public CountryRecordVigilanceDto countryInformation;
    public AirportRecordVigilanceDto airportFields;
    public OtherRecordVigilanceDto otherFields;
    public PersonRecordVigilanceDto personFields;
    public EntityRecordVigilanceDto entityFields;
    public VesselRecordVigilanceDto vesselFields;
    public SeaportRecordVigilanceDto seaportFields;


    public RecordVigilanceDto() {
    }

    private RecordVigilanceDto(RecordCommonVigilanceDto common) {

        this.generalFields = common;
    }

    public RecordVigilanceDto(RecordCommonVigilanceDto common, CityRecordVigilanceDto city) {
        this(common);
        this.cityFields = city;
    }

    public RecordVigilanceDto(RecordCommonVigilanceDto common, CountryRecordVigilanceDto country) {
        this(common);
        this.countryInformation = country;
    }

    public RecordVigilanceDto(RecordCommonVigilanceDto common, AirportRecordVigilanceDto airport) {
        this(common);
        this.airportFields = airport;
    }

    public RecordVigilanceDto(RecordCommonVigilanceDto common, OtherRecordVigilanceDto other) {
        this(common);
        this.otherFields = other;
    }

    public RecordVigilanceDto(RecordCommonVigilanceDto common, PersonRecordVigilanceDto person) {
        this(common);
        this.personFields = person;
    }

    public RecordVigilanceDto(RecordCommonVigilanceDto common, EntityRecordVigilanceDto entity) {
        this(common);
        this.entityFields = entity;
    }

    public RecordVigilanceDto(RecordCommonVigilanceDto common, VesselRecordVigilanceDto vessel) {
        this(common);
        this.vesselFields = vessel;
    }

    public RecordVigilanceDto(RecordCommonVigilanceDto common, SeaportRecordVigilanceDto seaport) {
        this(common);
        this.seaportFields = seaport;
    }

}

