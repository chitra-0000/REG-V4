package com.bnpp.regliss.entity.referential;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REF_GEOGRAPHIC_DATA")
/**@SequenceGenerator(name = "GeographicDataIdGenerator", initialValue = 100, sequenceName = "GEOGRAPHIC_DATA_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "GeographicDataIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "GEOGRAPHIC_DATA_SEQ")} )
public class GeographicData extends ListReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "GeographicDataIdGenerator")
    protected Long id;

    public GeographicData() {
    }

    public GeographicData(String label) { super(label); }

    public GeographicData(GeographicData other) { super(other); }
}
