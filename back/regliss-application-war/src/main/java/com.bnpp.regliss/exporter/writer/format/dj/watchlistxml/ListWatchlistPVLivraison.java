package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml;

import com.bnpp.regliss.exporter.writer.format.fircosoftxml.VersionAliasStatsPV;
import com.bnpp.regliss.exporter.writer.format.fircosoftxml.VersionRecStatsPV;
import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name="list")
@ToString
public class ListWatchlistPVLivraison {
    @XmlElement(name="product_name")
    public String listName;
    @XmlElement(name="product_version")
    public String version;
    @XmlElement(name="name")
    public String name;
    @XmlElement(name="records")
    public RecordsPVLivraison records;
    @XmlElement(name="database")
    public RecordsPVLivraison databaseRecords;
    @XmlElement(name="exported")
    public RecordsPVLivraison filteredRecords;
    @XmlElement(name="publish_date")
    public String versionPublishDate;
    @XmlElement(name="official_date")
    public String versionOffDate;
    @XmlTransient
    public Long versionId;
}
