package com.bnpp.regliss.entity.referential;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REF_LIST_CATEGORY")
/**@SequenceGenerator(name = "ListCategoryIdGenerator", initialValue = 100, sequenceName = "REF_LIST_CATEGORY_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "ListCategoryIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "REF_LIST_CATEGORY_SEQ")} )
public class ListCategory extends ListReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "ListCategoryIdGenerator")
    protected Long id;

    public ListCategory() {
    }

    public ListCategory(String label) { super(label); }

    public ListCategory(ListCategory other) { super(other); }
}
