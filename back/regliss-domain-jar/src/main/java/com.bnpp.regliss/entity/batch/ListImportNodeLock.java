package com.bnpp.regliss.entity.batch;

import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.ReglissList;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "CTR_LIST_IMPORT_NODE_LOCK")
@GenericGenerator( name = "ListImportNodeLockIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "CTR_LIST_IMPORT_NODE_LOCK_SEQ")} )
public class ListImportNodeLock extends AbstractSimpleEntity {
    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "ListImportNodeLockIdGenerator")
    /**@SequenceGenerator(name = "ListImportNodeLockIdGenerator", sequenceName = "CTR_LIST_IMPORT_NODE_LOCK_SEQ", allocationSize = 4)*/
    protected Long id;

    @Getter
    @ManyToOne
    @JoinColumn(name = "LIST_ID", unique = true)
    private ReglissList list;

    @Getter
    @Column(name = "NODE_ID",length = 64)
    private String nodeName ;

    public ListImportNodeLock() {}
    public ListImportNodeLock(ReglissList list,String nodeName) {
        this.list=list;
        this.nodeName=nodeName;
    }
}
