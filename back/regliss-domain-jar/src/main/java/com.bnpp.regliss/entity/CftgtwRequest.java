package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "CFT_GTW_REQUEST")
/**@SequenceGenerator(name = "CftgtwRequestIdGenerator", initialValue = 100, sequenceName = "CFT_GTW_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "CftgtwRequestIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "CFT_GTW_SEQ")} )
public class CftgtwRequest extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "CftgtwRequestIdGenerator")
    protected Long id;

    @Getter @Setter
    @EqualityField
    @Column(name = "DJ_FORMAT")
    @Enumerated(EnumType.STRING)
    private ImportFileType importFileType;

    @Getter @Setter
    @EqualityField
    @Column(name = "REQUEST_TIME")
    private LocalDateTime requestTime;

    @Getter @Setter
    @EqualityField
    @Column(name = "ACTUAL_RUNTIME")
    private LocalDateTime actualRunTime;

    @Getter @Setter
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "REMAINING_CFT_LOG_TIME_IDS")
    private String remainingCftLogTimeIds;

    public CftgtwRequest() {
    }

    public CftgtwRequest(ImportFileType importFileType, LocalDateTime requestTime, User user) {
        super();
        this.importFileType = importFileType;
        this.requestTime = requestTime;
        this.user = user;
    }

    public void setRemainingCftLogTimeIds(List<String> cftLogTimeIds) {
        this.remainingCftLogTimeIds = cftLogTimeIds.stream().collect(joining(","));
    }

    public List<String> getRemainingCftLogTimeIds() {
        return Stream.of(this.remainingCftLogTimeIds.split(",")).collect(toList());
    }

}
