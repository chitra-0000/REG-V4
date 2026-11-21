package com.bnpp.regliss.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;

@Entity
@Table(name = "REFOG_COUNTRY_MAPPING")
/**@SequenceGenerator(name = "RefogMappingIdGenerator", sequenceName = "REFOG_COUNTRY_MAPPING_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "RefogMappingIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "REFOG_COUNTRY_MAPPING_SEQ") } )
public class RefogCountryMapping {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "RefogMappingIdGenerator")
    protected Long id;

    @Getter
    @Setter
    @Column(name = "ISO2_CODE", length = 6)
    private String isoCode;

    @Getter
    @Setter
    @Column(name = "REFOG_LABEL")
    private String refogLabel;

    public RefogCountryMapping() {
    }

    public RefogCountryMapping(String isoCode, String refogLabel) {
        this.isoCode = isoCode;
        this.refogLabel = refogLabel;
    }
}
