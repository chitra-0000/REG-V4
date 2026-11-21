package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.entity.referential.FileExportMode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.Optional;

import static com.bnpp.regliss.entity.referential.FileExportFormat.*;
import static java.util.Optional.ofNullable;

@Entity
@Table(name = "GENERATED_FILE")
/**@SequenceGenerator(name = "GeneratedFileIdGenerator", initialValue = 100, sequenceName = "GENERATED_FILE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "GeneratedFileIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "GENERATED_FILE_SEQ")} )
public class GeneratedFile extends ExportFile {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "GeneratedFileIdGenerator")
    protected Long id;

    @Getter
    @Setter
    @EqualityField
    @Embedded
    private GeneratedFileFormat generatedFileFormat;

    @Setter
    @ManyToOne
    @EqualityField
    @JoinColumn(name = "SUBSCRIBER_ID")
    private Subscriber subscriber;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "CONCATENATED_LIST_ID")
    private ConcatenatedList concatenatedList;

    public GeneratedFile() { super(); }

    public GeneratedFile(GeneratedFileFormat generatedFileFormat, Subscriber subscriber, ConcatenatedList concatenatedList) {
        this.generatedFileFormat = generatedFileFormat;
        this.subscriber = subscriber;
        this.concatenatedList = concatenatedList;
    }

    /** @return non empty for PV */
    public Optional<Subscriber> getSubscriberOpt() { return ofNullable(subscriber); }

    /** @return empty for OFAC */
    public Optional<Version> getVersionOpt() { return ofNullable(getVersion()); }

    public Optional<ConcatenatedList> getConcatenatedListOpt() { return ofNullable(concatenatedList); }

    public boolean isFircosoftZip() {
        return generatedFileFormat.getFileFormat() == FileExportFormat.FIRCOSOFT && isZip();
    }

    public boolean isZip() { return generatedFileFormat.getFileExtension() == FileExportExtension.zip; }

    public boolean isDeliveryReport() { return generatedFileFormat.getFileFormat() == DELIVERY_REPORT; }

    public boolean isAFullAllFilter(boolean hasPreviousVersion) {
        if (!isDJAmeFormat() && !isDJWatchlistFormat()) {
            return false;
        }
        if (generatedFileFormat.getFilter() == null) {
            return false;
        }
        return generatedFileFormat.getFileExportMode().getActualExportMode(hasPreviousVersion) == FileExportMode.FULL
                && generatedFileFormat.getFilter().exportsAll();
    }

    public String getFullName() { return getFileName() + "." + generatedFileFormat.getFileExtension(); }

    public boolean isGeneratedForAFilter() {
        return generatedFileFormat.getFileFormat() == DJ_WATCHLIST || generatedFileFormat.getFileFormat() == DJ_AME;
    }

    public boolean isDJCoreFormat() { return generatedFileFormat.getFileFormat() == DJ_CORE; }

    public boolean isDJWatchlistFormat() { return generatedFileFormat.getFileFormat() == DJ_WATCHLIST; }

    public boolean isNotDJWatchlistFormat() { return !isDJWatchlistFormat(); }

    public boolean isDJAmeFormat() { return generatedFileFormat.getFileFormat() == DJ_AME; }

    public boolean isDJFormat() { return generatedFileFormat.getFileFormat().isDJFormat(); }

    public boolean isRegularFormat() { return !isDJFormat(); }

    public FileExportMode getFileExportMode() { return generatedFileFormat.getFileExportMode(); }

    public FileExportFormat getFileExportFormat() { return generatedFileFormat.getFileFormat(); }

    public enum GenerationStatus {
        OK, IN_PROGRESS, KO
    }

    public boolean isGeneratedForOfac() { return getVersion() == null; }

    public boolean shouldBeGenerated(boolean hasRecords) {
        if(isDJFormat()){
            return true;
        }
        return (hasRecords ||
                getGeneratedFileFormat().getFileExportMode() == FileExportMode.FULL ||
                getGeneratedFileFormat().getFileFormat().isIncrementalEmptyFileGenerated());
    }

    public boolean isWatchlistZip() { return generatedFileFormat.getFileFormat() == DJ_WATCHLIST && isZip(); }

}
