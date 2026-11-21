package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.entity.record.PersonRecord;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.record.RecordAwareChild;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "REF_GENDER")
/**@SequenceGenerator(name = "GenderIdGenerator", initialValue = 100, sequenceName = "GENDER_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "GenderIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "GENDER_SEQ")} )
public class Gender extends RecordReferential implements RecordAwareChild{

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "GenderIdGenerator")
    protected Long id;
    @ManyToMany(mappedBy = "genders")
    private List<PersonRecord> personRecords = new ArrayList<>();

    public Gender() {
    }

    public Gender(String label) { super(label); }

    public Gender(String label, ReferentialBusinessConcept businessConcept) { super(label, businessConcept); }

    public Gender(Gender otherGender) { super(otherGender); }

    public void addPersonRecord(PersonRecord personRecord) { personRecords.add(personRecord); }

    public void removePersonRecord(PersonRecord personRecord) { personRecords.remove(personRecord); }

    @Override
    public List<Record> getRecords() { return Collections.unmodifiableList(personRecords); }

    @Override
    public void removeFromParent(Record parentToUnlink) {
        PersonRecord pr = (PersonRecord) parentToUnlink;
        pr.removeGender(this);
    }

}
