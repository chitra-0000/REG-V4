package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "RECORD_DJ")
/**@SequenceGenerator(name = "RecordDJIdGenerator", initialValue = 100, sequenceName = "GENERATED_RECORD_DJ_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "RecordDJIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "GENERATED_RECORD_DJ_SEQ")} )
public class RecordDJ extends AbstractSimpleEntity implements ToStringAuditable{

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "RecordDJIdGenerator")
    protected Long id;
    @EqualityField
    @Setter @Getter
    @Column(name = "XML_SNIPPET")
    @Lob
    private String xmlSnippet;

    @EqualityField
    @Setter @Getter
    @Column(name = "ASSOCIATION_XML")
    @Lob
    private String associationXml;

    @Setter @Getter
    @Column(name = "CSV_SNIPPET")
    @Lob
    private String csvSnippet;

    public RecordDJ() {
        //intentionally left empty
    }


    @Override
    public String toStringAudit() { return "Id= " + id; }

    @Override
    public String toString() {
        return "RecordDJ [xmlSnippetCreatedUpdated=" + xmlSnippet + ", csvSnippetCreatedUpdate=" + csvSnippet + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof RecordDJ))
            return false;
        RecordDJ other = (RecordDJ) obj;
        if (getXmlSnippet() == null) {
            return other.getXmlSnippet() == null;
        }
        return getXmlSnippet().equals(other.getXmlSnippet());
    }

    @Override
    public int hashCode() { return getXmlSnippet().hashCode(); }
}
