package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import com.bnpp.regliss.vo.FeaturePermission;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static javax.persistence.CascadeType.ALL;

@Entity
@Table(name = "USERS",
        indexes = @Index(columnList = "PROFILE"))
/**@SequenceGenerator(name = "UserIdGenerator", initialValue = 100, sequenceName = "USER_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "UserIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "USER_SEQ") } )
public class User extends AbstractSimpleEntity implements Serializable {

    public static final String SYS_USERNAME = "sys";
    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UserIdGenerator")
    protected Long id;

    public enum Status {
        ACTIVE, INACTIVE
    }

    public enum LastConnectionType{
        FREE, SECURED
    }

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.USER_UID)
    @Column(name = "USERNAME", length=6)
    private String username;

    @Column(name = "USERNAME_NORMALIZED", length=12)
    private String usernameNormalized;

    @Getter
    @AuditedField(AuditedFieldCode.USER_FIRST_NAME)
    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "FIRST_NAME_NORMALIZED", length=450)
    private String firstNameNormalized;

    @Getter
    @AuditedField(AuditedFieldCode.USER_LAST_NAME)
    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "LAST_NAME_NORMALIZED", length=450)
    private String lastNameNormalized;

    @Getter @Setter
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @AuditedField(AuditedFieldCode.USER_LANGUAGE)
    @Column(name = "USER_LANGUAGE")
    private Language language = Language.EN;

    @Getter
    @AuditedField(AuditedFieldCode.USER_ENTITY)
    @Column(name = "ENTITY")
    private String entity;

    @Column(name = "ENTITY_NORMALIZED", length=450)
    private String entityNormalized;

    @Getter
    @AuditedField(AuditedFieldCode.USER_BUSINESS_LINE)
    @Column(name = "BUSINESS_LINE")
    private String businessLine;

    @Column(name = "BUSINESS_LINE_NORMALIZED", length=450)
    private String businessLineNormalized;

    @Getter
    @AuditedField(AuditedFieldCode.USER_POLE)
    @Column(name = "POLE")
    private String pole;

    @Column(name = "POLE_NORMALIZED", length=450)
    private String poleNormalized;

    @Getter @Setter
    @ManyToOne
    @AuditedField(AuditedFieldCode.USER_COUNTRY_NAME)
    @JoinColumn(name = "COUNTRY_ID")
    private Country country;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.USER_EMAIL)
    @Column(name = "EMAIL")
    private String email;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @AuditedField(AuditedFieldCode.USER_PROFILE)
    @Column(name = "PROFILE")
    private Profile profile;

    @Setter
    @AuditedField(AuditedFieldCode.USER_DOWNLOAD_EXPORT_PERMISSION_END_DATE)
    @Column(name = "EXPORTED_FILE_DOWNLOAD_ENDDATE")
    private LocalDate exportedFileDownloadPermissionEndDate;

    @Setter
    @AuditedField(AuditedFieldCode.USER_DOWNLOAD_IMPORT_PERMISSION_END_DATE)
    @Column(name = "IMPORTED_FILE_DOWNLOAD_ENDDATE")
    private LocalDate importedFileDownloadPermissionEndDate;

    @AuditedCollection(createAction = AuditedAction.USER_ADD_PERMISSION,
            updateAction = AuditedAction.NOT_APPLICABLE,
            deleteAction = AuditedAction.USER_REMOVE_PERMISSION)
    @OneToMany(mappedBy = "user", orphanRemoval = true, cascade = ALL)
    private Set<UserListPermission> assignedListPermissions = new HashSet<>();

    @Getter @Setter
    @Column (name = "LAST_CONNECTION")
    private LocalDateTime lastConnection;

    @Getter @Setter
    @Column (name = "DATE_UPDATE")
    private LocalDateTime dateOfUpdate;

    public User() {
    }

    public User(String username) { setUsername(username); }

    public User(User other) {
        this.id = other.id;
        this.firstName = other.firstName;
        this.firstNameNormalized = other.firstNameNormalized;
        this.language = other.language;
        this.lastName = other.lastName;
        this.lastNameNormalized = other.lastNameNormalized;
        this.status = other.status;
        this.username = other.username;
        this.usernameNormalized = other.usernameNormalized;
        this.businessLine = other.businessLine;
        this.businessLineNormalized = other.businessLineNormalized;
        this.country = other.country;
        this.email = other.email;
        this.entity = other.entity;
        this.entityNormalized = other.entityNormalized;
        this.pole = other.pole;
        this.poleNormalized = other.poleNormalized;
        this.profile = other.profile;
        this.exportedFileDownloadPermissionEndDate = other.exportedFileDownloadPermissionEndDate;
        this.importedFileDownloadPermissionEndDate = other.importedFileDownloadPermissionEndDate;
        this.assignedListPermissions.addAll(other.assignedListPermissions);
        this.lastConnection = other.lastConnection;
        this.dateOfUpdate = other.dateOfUpdate;
    }


    public boolean isActive() { return status == Status.ACTIVE; }

    public String getFullName() { return (firstName!=null?firstName:"") + " " + (lastName!=null?lastName:""); }

    public Set<UserListPermission> getAssignedListPermissions() { return unmodifiableSet(assignedListPermissions); }

    public void setAssignedListPermissions(Set<UserListPermission> permissions) {
        this.assignedListPermissions.clear();
        permissions.forEach(this :: addPermission);
    }

    private void addPermission(UserListPermission permission){
        assignedListPermissions.add(permission);
        permission.setUser(this);
    }

    public Optional<LocalDate> getExportedFileDownloadPermissionEndDateOpt(){
        return Optional.ofNullable(exportedFileDownloadPermissionEndDate);
    }

    public Optional<LocalDate> getImportedFileDownloadPermissionEndDateOpt(){
        return Optional.ofNullable(importedFileDownloadPermissionEndDate);
    }

    public boolean hasExportedFileDownloadPermissionNow() {
        return exportedFileDownloadPermissionEndDate != null &&
                exportedFileDownloadPermissionEndDate.isAfter(LocalDate.now());
    }

    public boolean hasImportedFileDownloadPermissionNow() {
        return importedFileDownloadPermissionEndDate != null &&
                importedFileDownloadPermissionEndDate.isAfter(LocalDate.now());
    }

    public boolean hasPermission(FeaturePermission permission) {
        return profile != null && profile.getBaseFeatures().contains(permission);
    }

    public void setUsername(String username) {
        this.username = username;
        this.usernameNormalized = TextNormalizerQueryUtil.normalize(username);
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.firstNameNormalized = TextNormalizerQueryUtil.normalize(firstName);
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.lastNameNormalized = TextNormalizerQueryUtil.normalize(lastName);
    }

    public void setEntity(String entity) {
        this.entity = entity;
        this.entityNormalized = TextNormalizerQueryUtil.normalize(entity);
    }

    public void setBusinessLine(String businessLine) {
        this.businessLine = businessLine;
        this.businessLineNormalized = TextNormalizerQueryUtil.normalize(businessLine);
    }

    public void setPole(String pole) {
        this.pole = pole;
        this.poleNormalized = TextNormalizerQueryUtil.normalize(pole);
    }
}
