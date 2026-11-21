package com.bnpp.regliss.entity.batch;

import com.bnpp.regliss.entity.AbstractSimpleEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CTR_BATCH_EXPORT")
/**@SequenceGenerator(name = "BatchExportIdGenerator", initialValue = 100, sequenceName = "CTR_BATCH_EXPORT_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "BatchExportIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "CTR_BATCH_EXPORT_SEQ")
        } )
public class BatchExport extends AbstractSimpleEntity{
    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "BatchExportIdGenerator")
    protected Long id;

    @Getter @Setter
    @Column(name = "BATCH_NODE_ID")
    private String batchNodeId;

    @Getter
    @Column(name = "REQUEST_TIME", nullable=false)
    private LocalDateTime requestTime;

    @Getter @Setter
    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Transient
    @Getter
    @Setter
    private long batchExecutionId;

    @Getter
    @Embedded
    private BatchExportParams exportParams = new BatchExportParams();

    public BatchExport(){}

    public BatchExport(BatchExportParams exportParams){
        this.exportParams = exportParams;
        this.requestTime = LocalDateTime.now();
    }
}
