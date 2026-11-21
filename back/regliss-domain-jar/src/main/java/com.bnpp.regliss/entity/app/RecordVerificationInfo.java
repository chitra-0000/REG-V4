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
@Table(name = "APP_RECORD_VERIFICATION_INFO")
@NoArgsConstructor
@GenericGenerator( name = "RecordVerificationInfoIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "RECORD_VERIFICATION_INFO_SEQ") } )
public class RecordVerificationInfo extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "RecordVerificationInfoIdGenerator")
    /** @SequenceGenerator(name = "RecordVerificationInfoIdGenerator", sequenceName = "RECORD_VERIFICATION_INFO_SEQ", allocationSize = 1)*/
    protected Long id;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "RECORD_ID", length = 13)
    private Long recordId;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "CHILD_ID")
    private Long childId;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "CHILD_TYPE")
    @Enumerated(EnumType.STRING)
    private ChildType childType;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "AUDITED_ACTION")
    @Enumerated(EnumType.STRING)
    private AuditedAction auditedAction;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "VERSION_ID")
    private Long versionId;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_VERIFICATION_COMMENT)
    @Column(name = "VERIFICATION_COMMENT", length = 2048)
    private String verificationComment;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_VERIFICATION_STATUS)
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

    public static RecordVerificationInfo emptyWithIds(
            Long versionId,
            Long recordId,
            Long childId,
            ChildType childType,
            AuditedAction auditedAction
    ) {
        return new RecordVerificationInfo(versionId, recordId, childId, childType, auditedAction);
    }

    public RecordVerificationInfo(RecordVerificationInfo other) {
        this.id = other.id;
        this.versionId = other.versionId;
        this.recordId = other.recordId;
        this.childId = other.childId;
        this.childType = other.childType;
        this.auditedAction = other.auditedAction;

        this.verificationComment = other.verificationComment;
        this.verificationStatus = other.verificationStatus;
    }

    private RecordVerificationInfo(
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
}
