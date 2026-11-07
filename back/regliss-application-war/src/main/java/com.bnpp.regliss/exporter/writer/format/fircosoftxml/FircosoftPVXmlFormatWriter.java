package com.bnpp.regliss.exporter.writer.format.fircosoftxml;

import com.bnpp.regliss.ReglissException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.io.Writer;

@Slf4j
public class FircosoftPVXmlFormatWriter {

    private static final JAXBContext JAXB_CONTEXT;

    private FircosoftPVXmlFormatWriter() {
    }

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(ListsPVLivraison.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SneakyThrows
    public static void generateFircosoftPvXml(ListsPVLivraison allListPV, Writer bw) {
        try {
            Marshaller jaxbMarshaller = JAXB_CONTEXT.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");

            StringWriter sw = new StringWriter();
            jaxbMarshaller.marshal(allListPV, sw);
            bw.write(sw.toString());
        } catch (JAXBException e) {
            throw new ReglissException(e);
        }
    }

}

