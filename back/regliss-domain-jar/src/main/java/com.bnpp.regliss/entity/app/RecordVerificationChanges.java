package com.bnpp.regliss.entity.app;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.record.VerificationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "APP_RECORD_VERIF_CHANGES")
@NoArgsConstructor
@GenericGenerator( name = "RecordVerificationChangesIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "RECORD_VERIF_CHANGES_SEQ") } )
public class RecordVerificationChanges extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "RecordVerificationChangesIdGenerator")
    /**@SequenceGenerator(name = "RecordVerificationChangesIdGenerator", sequenceName = "RECORD_VERIF_CHANGES_SEQ", allocationSize = 1)*/
    protected Long id;

    @Getter
    @Setter
    @Column(name = "RECORD_ID", length = 13)
    private Long recordId;

    @Getter
    @Setter
    @Column(name = "CHILD_ID")
    private Long childId;

    // TODO: [GAP] lines 41-41 not captured in screenshots — do not fabricate missing logic.

    @Getter
    @Setter
    @Column(name = "CHILD_TYPE")
    @Enumerated(EnumType.STRING)
    private ChildType childType;

    @Getter
    @Setter
    @Column(name = "AUDITED_ACTION")
    @Enumerated(EnumType.STRING)
    private AuditedAction auditedAction;

    @Getter
    @Setter
    @Column(name = "VERSION_ID")
    private Long versionId;

    @Getter
    @Setter
    @Column(name = "VERIFICATION_COMMENT", length = 2048)
    private String verificationComment;

    @Getter
    @Setter
    @Column(name = "VERIFICATION_STATUS")
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    @Getter
    @Setter
    @Column(name = "USER_ID")
    private Long userId;

    @Getter
    @Setter
    @Column(name = "SAVE_DATE")
    private LocalDateTime saveDate;

    public static RecordVerificationChanges emptyWithIds(
            Long versionId,
            Long recordId,
            Long childId,
            ChildType childType,
            AuditedAction auditedAction
    ) {
        return new RecordVerificationChanges(versionId, recordId, childId, childType, auditedAction);
    }

    public RecordVerificationChanges(RecordVerificationChanges other) {
        this.id = other.id;
        this.versionId = other.versionId;
        this.recordId = other.recordId;
        this.childId = other.childId;
        this.childType = other.childType;
        this.auditedAction = other.auditedAction;

        this.verificationComment = other.verificationComment;
        this.verificationStatus = other.verificationStatus;
    }

    private RecordVerificationChanges(
            Long versionId,
            Long recordId,
            Long childId,
            ChildType childType,
            AuditedAction auditedAction
    ) {
        this.versionId = versionId;
        this.recordId = recordId;
        this.childId = childId;
        this.childType = childType;
        this.auditedAction = auditedAction;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof RecordVerificationChanges))
            return false;
        RecordVerificationChanges other = (RecordVerificationChanges) obj;
        if ((recordId != other.getRecordId()))
            return false;
        if ((childId != other.getChildId()))
            return false;
        if ((childType != other.getChildType()))
            return false;
        if ((auditedAction != other.getAuditedAction()))
            return false;
        if ((versionId != other.getVersionId()))
            return false;
        if ((verificationStatus != other.getVerificationStatus()))
            return false;
        if (verificationComment == null){
            if (other.getVerificationComment() != null)
                return false;
        } else if ( !verificationComment.equals(other.getVerificationComment())){
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((recordId == null) ? 0 : recordId.hashCode());
        result = prime * result + ((childId == null) ? 0 : childId.hashCode());
        result = prime * result + ((childType == null) ? 0 : childType.hashCode());
        result = prime * result + ((auditedAction == null) ? 0 : auditedAction.hashCode());
        result = prime * result + ((versionId == null) ? 0 : versionId.hashCode());
        result = prime * result + ((verificationStatus == null) ? 0 : verificationStatus.hashCode());
        result = prime * result + ((verificationComment == null) ? 0 : verificationComment.hashCode());
        return result;
    }
}
