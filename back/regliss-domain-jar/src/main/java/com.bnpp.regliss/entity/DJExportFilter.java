package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.vo.export.dj.watchlist.FilterIdTag;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.*;

import static java.util.stream.Collectors.toSet;

@Entity
@Table(name = "DJ_EXPORT_FILTER")
/**@SequenceGenerator(name = "DJExportFilterIdGenerator", initialValue = 100, sequenceName = "DJ_EXPORT_FILTER_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "DJExportFilterIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "DJ_EXPORT_FILTER_SEQ") } )
public class DJExportFilter extends AbstractSimpleEntity implements ToStringAuditable{

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "DJExportFilterIdGenerator")
    protected Long id;
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.FILTER_NAME)
    @Column(name = "FILTER_NAME")
    private String filterName;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.FILTER_TAG)
    @Column(name = "FILTER_TAG", length = 15, nullable=false)
    private String filterTag;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.FILTER_COMMENT)
    @Column(name = "COMMENTS", length = 2000)
    private String comment;

    @Getter @Setter
    @EqualityField
    @Column(name = "IS_ACTIVE")
    private boolean active;

    @Getter @Setter
    @EqualityField
    @Column(name = "GENERATE_EXPORT_FILES")
    private boolean generateExportFiles;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.FILTER_TYPE)
    @Column(name = "FILE_TYPE")
    @Enumerated(EnumType.STRING)
    private FileExportMode type;

    @Getter @Setter
    @ElementCollection(targetClass = FileExportExtension.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FILTER_EXTENSION", joinColumns = @JoinColumn(name = "filter_id"))
    @Column(name = "file_extension")
    @AuditedField(AuditedFieldCode.FILTER_EXTENSION)
    private Set<FileExportExtension> extensions = new HashSet<>();

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.FILTER_FORMAT)
    @Column(name = "FILTER_FORMAT")
    @Enumerated(EnumType.STRING)
    private ImportFileType format;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "LIST_ID")
    private ReglissList list;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.FILTER_DELETE)
    @Column(name = "IS_DELETED")
    private boolean deleted = false;

    @AuditedCollection(createAction = AuditedAction.LIST_FILTER_SUBFILTER_ADDITION,
            updateAction = AuditedAction.LIST_FILTER_SUBFILTER_MODIFICATION,
            deleteAction = AuditedAction.LIST_FILTER_SUBFILTER_DELETION)
    @OneToMany(mappedBy = "filter", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DJExportSubfilter> subfilters = new HashSet<>();

    public DJExportFilter() {
    }

    public DJExportFilter(DJExportFilter other) {
        this.id = other.id;
        this.filterName = other.filterName;
        this.filterTag = other.filterTag;
        this.type = other.type;
        this.format = other.format;
        this.active = other.active;
        this.comment = other.comment;
        this.generateExportFiles = other.generateExportFiles;
        this.deleted = other.deleted;
        for (DJExportSubfilter otherSubfilter : other.subfilters) {
            addSubfilter(new DJExportSubfilter(otherSubfilter));
        }
        for (FileExportExtension extension:other.extensions){
            addExtension(extension);
        }
    }

    public void addExtension(FileExportExtension fileExportExtension) { this.extensions.add(fileExportExtension); }

    public void addSubfilter(DJExportSubfilter subFilter) {
        subFilter.setFilter(this);
        this.subfilters.add(subFilter);
        if (subFilter.getIndex() == null) {
            subFilter.setIndex(getNextSubfilterIndex());
        }
    }

    public void updateSubfilter(DJExportSubfilter subFilter) { this.subfilters.add(subFilter); }

    public void removeSubfilter(DJExportSubfilter filter) { filter.setDeleted(true); }


    public Set<DJExportSubfilter> getActiveSubfilters() {
        return Collections.unmodifiableSet(this.subfilters.stream()
                .filter(DJExportSubfilter::isNotDeleted)
                .collect(toSet()));
    }

    public Set<DJExportSubfilter> getSubfilters() { return Collections.unmodifiableSet(this.subfilters); }

    public boolean isNotActive() { return !active; }

    @Override
    public String toStringAudit() { return filterName; }

    public Integer getNextSubfilterIndex() {
        return 1 + subfilters.stream()
                .map(DJExportSubfilter::getIndex)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    public Optional<DJExportSubfilter> getSubfilterByIndex(Integer index) {
        return subfilters.stream()
                .filter(sf -> index.equals(sf.getIndex()))
                .findAny();
    }

    public boolean exportsAll() {
        return subfilters.stream().filter(DJExportSubfilter::isNotDeleted)
                .anyMatch(DJExportSubfilter::isAll);
    }

    public FilterIdTag getFilterIdTag() { return new FilterIdTag(getId(),getFilterTag()); }
}
