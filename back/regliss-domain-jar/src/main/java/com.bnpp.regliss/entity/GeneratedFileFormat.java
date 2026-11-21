package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.entity.referential.FileExportMode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Embeddable
public class GeneratedFileFormat implements ToStringAuditable{

    @Getter
    @EqualityField
    @Column(name = "FILE_FORMAT")
    @Enumerated(EnumType.STRING)
    private FileExportFormat fileFormat;

    @Getter
    @EqualityField
    @Column(name = "FILE_TYPE")
    @Enumerated(EnumType.STRING)
    private FileExportMode fileExportMode;

    @Getter
    @EqualityField
    @Column(name = "FILE_EXTENSION")
    @Enumerated(EnumType.STRING)
    private FileExportExtension fileExtension;

    @Getter @Setter
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "FILTER_ID")
    private DJExportFilter filter;

    protected GeneratedFileFormat() {}

    public GeneratedFileFormat(FileExportFormat fileFormat, FileExportMode fileExportMode, FileExportExtension fileExtension) {
        this.fileFormat = fileFormat;
        this.fileExportMode = fileExportMode;
        this.fileExtension = fileExtension;
    }

    public GeneratedFileFormat(DJExportFilter filter,FileExportExtension extension) {
        this(determineFormat(filter), filter.getType(), extension);
        this.filter = filter;
    }

    private static FileExportFormat determineFormat(DJExportFilter filter) {
        switch (filter.getFormat()) {
        case DOW_JONES_AME:
            return FileExportFormat.DJ_AME;
        case DOW_JONES_WATCHLIST:
            return FileExportFormat.DJ_WATCHLIST;
        default:
            throw new IllegalArgumentException();
        }
    }

    public GeneratedFileFormat(GeneratedFileFormat other) {
        this.fileExtension = other.fileExtension;
        this.fileFormat = other.fileFormat;
        this.fileExportMode = other.fileExportMode;
        this.filter = other.filter;
    }

    @Override
    public String toStringAudit() {
        String filterStringAudit = filter != null ? (", filter= " + filter.getFilterName()) : "" ;
        return "{ fileExtension= " + fileExtension.name() + ", fileFormat= " + fileFormat.name() + ", fileExportMode= "
                + fileExportMode.name()
                + filterStringAudit
                + " }";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFileExportMode() == null) ? 0 : getFileExportMode().hashCode());
        result = prime * result + ((getFileExtension() == null) ? 0 : getFileExtension().hashCode());
        result = prime * result + ((getFileFormat() == null) ? 0 : getFileFormat().hashCode());
        result = prime * result + ((getFilter() == null) ? 0 : getFilter().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GeneratedFileFormat other = (GeneratedFileFormat) obj;
        if (getFileExportMode() != other.getFileExportMode())
            return false;
        if (getFileExtension() != other.getFileExtension())
            return false;
        if (getFileFormat() != other.getFileFormat())
            return false;
        if (getFilter() == null) {
            if (other.getFilter() != null)
                return false;
        } else if (!getFilter().equals(other.getFilter())) {
            return false;
        }
        return true;
    }

}
