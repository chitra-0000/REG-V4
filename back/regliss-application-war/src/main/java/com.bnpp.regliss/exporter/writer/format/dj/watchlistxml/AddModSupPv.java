package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml;

import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
@ToString
public class AddModSupPv {
    @XmlElement(name="count")
    public long count;
    @XmlElement(name="entities")
    public long entities;
    @XmlElement(name="persons")
    public long persons;
    @XmlTransient
    public String comment;


}
