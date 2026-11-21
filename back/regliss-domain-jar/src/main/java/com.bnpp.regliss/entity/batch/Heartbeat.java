package com.bnpp.regliss.entity.batch;

import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.CftType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CTR_HEARTBEAT")
@NoArgsConstructor
@GenericGenerator( name = "HeartbeatIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "HEARTBEAT_SEQ") } )
public class Heartbeat extends AbstractSimpleEntity {
    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "HeartbeatIdGenerator")
    /**@SequenceGenerator(name = "HeartbeatIdGenerator", sequenceName = "HEARTBEAT_SEQ", allocationSize = 2)*/
    protected Long id;

    @Getter
    @EqualityField
    @Column(name = "NODE_ID", unique = true)
    private String nodeId;

    @Getter
    @Setter
    @Column
    private String hostname;

    @Getter
    @Setter
    @Column
    private Integer port;

    // TODO: [GAP] lines 42-44 not captured in screenshots — do not fabricate missing logic.

    @Getter
    @Setter
    @Column(name = "ACTIVE_PROFILES")
    private String activeProfiles;

    @Getter
    @Setter
    @Column
    private long pid;

    @Getter
    @Setter
    @Column(name = "CFT_PUBLISHING_STATUS", unique = true)
    @Enumerated(EnumType.STRING)
    private CftPublishingStatus cftPublishingStatus;

    @Getter
    @Setter
    @Column(name = "CFT_TYPE")
    @Enumerated(EnumType.STRING)
    private CftType cftType;

    @Getter
    @Setter
    @Column(name = "LAST_BEAT")
    private LocalDateTime lastBeat;

    public Heartbeat(String nodeId) { this.nodeId = nodeId; }

    /**
     * This enum should always have one value: "ACTIVE".
     * This way, as the column in Heartbeat is unique, only one node can be active at a given time, all others being null
     */
    public enum CftPublishingStatus {
        ACTIVE
        // do not add any other values
    }
}
