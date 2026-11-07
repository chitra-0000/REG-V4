package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml;

import com.bnpp.regliss.ReglissException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.Writer;

@Slf4j
public class WatchlistPVXmlFormatWriter {

    private static final JAXBContext JAXB_CONTEXT;

    private WatchlistPVXmlFormatWriter() {
    }

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(ListsWatchlistPVLivraison.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SneakyThrows
    public static void generateXmlPVFile(ListsWatchlistPVLivraison allListPV, Writer bw) {
        try {
            Marshaller jaxbMarshaller = JAXB_CONTEXT.createMarshaller();
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            XMLStreamWriter xmlStreamWriter = factory.createXMLStreamWriter(bw);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            jaxbMarshaller.setListener(new WatchlistPVXmlFormatListner(xmlStreamWriter));
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
            jaxbMarshaller.marshal(allListPV, xmlStreamWriter);
            xmlStreamWriter.close();
        } catch (JAXBException e) {
            throw new ReglissException(e);
        }
    }


}
