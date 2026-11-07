package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml;

import com.bnpp.regliss.exporter.writer.format.fircosoftxml.ListPVLivraison;
import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


@XmlRootElement(name="lists")
@XmlType(propOrder= {"creationDate","uploadDate","list"})
@ToString
public class ListsWatchlistPVLivraison {
    @XmlElement(name="list")
    public List<ListWatchlistPVLivraison> list=new ArrayList<>();
    @XmlElement(name="date_creation")
    public String creationDate;
    @XmlElement(name="date_file_upload_gmt")
    public String uploadDate;

}
