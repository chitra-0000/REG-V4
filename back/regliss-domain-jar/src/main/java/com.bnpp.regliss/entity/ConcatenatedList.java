package com.bnpp.regliss.entity;

import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "CONCATENATED_LIST")
/**@SequenceGenerator(name = "ConcatenatedListIdGenerator", initialValue = 100, sequenceName = "CONCATENATED_LIST_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "ConcatenatedListIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "CONCATENATED_LIST_SEQ")} )
public class ConcatenatedList extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "ConcatenatedListIdGenerator")
    protected Long id;

    @Getter
    @Setter
    @Column(name = "LABEL", length = 255)
    private String label;

    @Getter
    @Setter
    @Column(name = "PUBLICATION_DATE")
    private LocalDateTime publicationDate;

    @Getter
    @Setter
    @Column(name = "IS_DELETED")
    private boolean deleted;

    @Getter
    @Setter
    @Column(name = "IS_ACTIVE")
    private boolean active;

    @Getter
    @Setter
    @Column(name = "GENERIC_FORMAT")
    private boolean genericFormat;

    @Getter
    @Setter
    @Column(name = "REGLISS_FORMAT")
    private boolean reglissFormat;

    @Getter
    @Setter
    @Column(name = "VIGILANCE_FORMAT")
    private boolean vigilanceFormat;

    @Getter
    @Setter
    @Column(name = "SAFEWATCH_FORMAT")
    private boolean safewatchFormat;

    @Getter
    @Setter
    @Column(name = "FIRCOSOFT_FORMAT")
    private boolean fircosoftFormat;

    @Getter
    @Setter
    @Column(name = "IS_FULLY_EDITABLE")
    private boolean fullyEditable;

    @Getter
    @Setter
    @Column(name = "BUSINESS_CONCEPT")
    @Enumerated(EnumType.STRING)
    protected ReferentialBusinessConcept businessConcept;

    @ManyToMany(cascade = CascadeType.MERGE)
    @JoinTable(name = "CONCATENATED_LIST_REGLISS_LIST", joinColumns = @JoinColumn(name="CONCATENATED_LIST_ID"), inverseJoinColumns = @JoinColumn(name="REGLISS_LIST_ID"))
    private Set<ReglissList> lists = new HashSet<>();

    @OneToMany(mappedBy = "concatenatedList", cascade = CascadeType.ALL)
    private final List<GeneratedFile> generatedFiles = new ArrayList<>();

    @OneToMany(mappedBy = "concatenatedList", cascade = CascadeType.ALL)
    private Set<Subscription> subscriptions = new HashSet<>();

    public ConcatenatedList() {
    }

    public ConcatenatedList(ConcatenatedList other) {
        this.id = other.id;
        this.label = other.label;
        this.publicationDate = other.publicationDate;
        this.deleted = other.deleted;
        this.active = other.active;
        this.genericFormat = other.genericFormat;
        this.reglissFormat = other.reglissFormat;
        this.vigilanceFormat = other.vigilanceFormat;
        this.safewatchFormat = other.safewatchFormat;
        this.fircosoftFormat = other.fircosoftFormat;
        this.fullyEditable = other.fullyEditable;
        this.lists.addAll(other.lists);
        this.generatedFiles.addAll(other.generatedFiles);
        this.subscriptions.addAll(other.subscriptions);
    }

    public Set<ReglissList> getLists() { return unmodifiableSet(lists); }

    public List<Long> getListsIds() {
        return lists.stream()
                .map(ReglissList::getId)
                .collect(toList());
    }

    public void addReglissList(ReglissList reglissList) { lists.add(reglissList); }

    public void setLists(Set<ReglissList> lists) {
        this.lists.clear();
        lists.forEach(this :: addList);
    }

    public void replaceReglissLists(Set<ReglissList> newReglissListsForConcatenatedList){
        if(!this.lists.isEmpty())
            this.lists.clear();
        this.lists.addAll(newReglissListsForConcatenatedList);
    }

    private void addList(ReglissList list) { lists.add(list); }

    public void removeList(ReglissList list) { lists.remove(list); }

    public List<GeneratedFile> getGeneratedFiles() {
        return Collections.unmodifiableList(this.generatedFiles.stream()
                .filter(gf -> gf.getGeneratedFileFormat().getFileFormat() != FileExportFormat.DELIVERY_REPORT)
                .collect(toList()));
    }

    public void addGeneratedFile(final GeneratedFile generatedFile) {
        generatedFiles.add(generatedFile);
        generatedFile.setConcatenatedList(this);
    }

    public List<FileExportFormat> getConcatenatedListExportFormats(){
        List<FileExportFormat> concatenatedListExportFormats = new ArrayList<>();
        if(genericFormat)
            concatenatedListExportFormats.add(FileExportFormat.GENERIC);
        if(reglissFormat)
            concatenatedListExportFormats.add(FileExportFormat.REGLISS);
        if(vigilanceFormat)
            concatenatedListExportFormats.add(FileExportFormat.VIGILANCE);
        if(safewatchFormat)
            concatenatedListExportFormats.add(FileExportFormat.SAFEWATCH);
        if(fircosoftFormat)
            concatenatedListExportFormats.add(FileExportFormat.FIRCOSOFT);
        return concatenatedListExportFormats;
    }

    public void deteleAllListsFromConcatenatedList(){
        if(!this.lists.isEmpty()){
            this.lists.clear();
        }
    }

    public List<Subscription> getSubscriptions(){
        return Collections.unmodifiableList(this.subscriptions.stream()
                .filter(sub-> !sub.isDeleted() && sub.getSubscriber().isActive())
                .collect(toList()));
    }

    public List<Subscriber> getSubscribers(){
        return Collections.unmodifiableList(this.subscriptions.stream()
                .filter(sub-> !sub.isDeleted() && sub.getSubscriber().isActive())
                .map(Subscription::getSubscriber)
                .distinct()
                .collect(toList()));
    }
}
