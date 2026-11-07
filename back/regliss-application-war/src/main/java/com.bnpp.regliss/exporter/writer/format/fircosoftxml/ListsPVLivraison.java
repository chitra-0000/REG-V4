package com.bnpp.regliss.exporter.writer.format.fircosoftxml;

// [MISSING FROM SCREENSHOTS: lines 3-9 of the original file were not visible in the photos]


@XmlRootElement(name = "lists")
@XmlType(propOrder = {"creationDate", "list"})
@ToString
public class ListsPVLivraison {
    @XmlElement(name="list")
    public List<ListPVLivraison> list=new ArrayList<>();
    @XmlElement(name="date_creation")
    public String creationDate;
}

