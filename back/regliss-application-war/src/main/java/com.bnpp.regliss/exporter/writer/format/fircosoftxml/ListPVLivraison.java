package com.bnpp.regliss.exporter.writer.format.fircosoftxml;

import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "list")
@ToString
public class ListPVLivraison {
    @XmlElement(name = "name")
    public String listName;
    @XmlElement(name = "ref_vigilance")
    public String refVigilance;
    @XmlElement(name = "version")
    public String version;
    @XmlElement(name = "main_records")
    public VersionRecStatsPV versionRecStatsPV;
    @XmlElement(name = "synonymes")
    public VersionAliasStatsPV versionAliasStatsPV;
    @XmlElement(name = "publish_date")
    public String versionPublishDate;
    @XmlElement(name="official_date")
    public String versionOffDate;
    @XmlTransient
    public Long versionId;
}

