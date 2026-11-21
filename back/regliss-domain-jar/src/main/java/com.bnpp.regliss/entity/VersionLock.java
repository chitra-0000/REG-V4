package com.bnpp.regliss.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

@Entity
@Table(name = "VERSION_LOCK")
/**@SequenceGenerator(name = "VersionLockIdGenerator", initialValue = 100, sequenceName = "VERSION_LOCK_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "VersionLockIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "VERSION_LOCK_SEQ")} )
public class VersionLock extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "VersionLockIdGenerator")
    protected Long id;
    @Getter @Setter
    @Column(name = "VERSION_ID")
    private Long versionId;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private User user;

    @Getter @Setter
    @Column(name = "ACQUIRE_DATE")
    private LocalDateTime acquireDate;

    @Getter @Setter
    @Column(name = "RELEASE_DATE")
    private LocalDateTime releaseDate;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "RELEASE_USER")
    private User releaseUser;

    public boolean isOwnedBy(Long userId) { return user != null && user.getId().equals(userId); }

    public boolean isActive() { return releaseDate.isAfter(now()); }

    public void extendReleaseDate(long minutes){
        if(releaseDate != null){
            releaseDate = now().plusMinutes(minutes);
        }
    }
}
