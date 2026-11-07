package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml;

import com.bnpp.regliss.ReglissException;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class WatchlistPVXmlFormatListner extends Marshaller.Listener {

    private final XMLStreamWriter xmlStreamWriter;

    public WatchlistPVXmlFormatListner(XMLStreamWriter xmlStreamWriter) { this.xmlStreamWriter = xmlStreamWriter; }

    @Override
    public void beforeMarshal(Object source) {
        try {
            if (source instanceof RecordsPVLivraison) {

                xmlStreamWriter.writeComment(((RecordsPVLivraison) source).comment);
            }
            if (source instanceof AddModSupPv) {
                AddModSupPv addModSupPv = (AddModSupPv) source;
                if (!StringUtils.isEmpty(addModSupPv.comment)) {
                    xmlStreamWriter.writeComment(addModSupPv.comment);
                }
            }

        } catch (XMLStreamException exception) {
            throw new ReglissException(exception);
        }

    }
}
