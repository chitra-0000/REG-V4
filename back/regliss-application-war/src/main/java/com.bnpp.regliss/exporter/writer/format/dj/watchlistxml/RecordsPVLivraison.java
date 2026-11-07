package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml;

import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
@ToString
public class RecordsPVLivraison {
    @XmlElement(name="count")
    public Long count;
    @XmlElement(name="included")
    public Long included;
    @XmlElement(name="add")
    public AddModSupPv add;
    @XmlElement(name="mod")
    public AddModSupPv mod;
    @XmlElement(name="sup")
    public AddModSupPv sup;
    @XmlElement(name="excluded")
    public AddModSupPv excluded;
    @XmlTransient
    public String comment;
}
