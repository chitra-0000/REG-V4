package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REF_IDENTITY_DOCUMENT_TYPE")
/**@SequenceGenerator(name = "IdentityDocumentTypeIdGenerator", initialValue = 100, sequenceName = "ID_DOC_TYPE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "IdentityDocumentTypeIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "ID_DOC_TYPE_SEQ")} )
public class IdentityDocumentType extends RecordReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "IdentityDocumentTypeIdGenerator")
    protected Long id;

    public IdentityDocumentType() {
    }

    public  IdentityDocumentType(String label) { super(label); }

    public IdentityDocumentType(IdentityDocumentType other) { super(other); }

    public IdentityDocumentType(String label, ReferentialBusinessConcept businessConcept) {
        super(label, businessConcept);
    }
}
