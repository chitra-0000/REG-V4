package com.bnpp.regliss.exporter.writer.format.fircosoftxml;

import javax.xml.bind.annotation.XmlElement;

public class VersionRecStatsPV {
    @XmlElement(name="count")
    public long recordTotalCount;
    @XmlElement(name="add")
    public long recordAddCount;
    @XmlElement(name="mod")
    public long recordModCount;
    @XmlElement(name="sup")
    public long recordSuppCount;
}

