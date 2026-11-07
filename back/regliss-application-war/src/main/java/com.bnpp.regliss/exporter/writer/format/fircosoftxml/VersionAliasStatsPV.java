package com.bnpp.regliss.exporter.writer.format.fircosoftxml;

import javax.xml.bind.annotation.XmlElement;

public class VersionAliasStatsPV {
    @XmlElement(name="count")
    public long aliasTotalCount;
    @XmlElement(name="strong")
    public long aliasStrongCount;
    @XmlElement(name="weak")
    public long aliasWeakCount;
    @XmlElement(name="add")
    public long aliasAddCount;
    @XmlElement(name="mod")
    public long aliasModCount;
    @XmlElement(name="sup")
    public long aliasSuppCount;
}

