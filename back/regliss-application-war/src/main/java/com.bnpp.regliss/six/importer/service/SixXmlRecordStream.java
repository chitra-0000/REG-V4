package com.bnpp.regliss.six.importer.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class SixXmlRecordStream {

    public interface RecordHandler<D> {
        void onRecord(D dto);
    }

    private final XmlMapper xmlMapper;

    public SixXmlRecordStream(XmlMapper xmlMapper) {
        this.xmlMapper = xmlMapper;
        XMLInputFactory factory = inputFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    }

    public int count(File xml, String recordElement) throws IOException, XMLStreamException {
        int count = 0;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(xml.toPath()), 1 << 16)) {
            XMLStreamReader reader = inputFactory().createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT
                            && recordElement.equals(reader.getLocalName())) {
                        count++;
                    }
                }
            } finally {
                reader.close();
            }
        }
        return count;
    }

    public <D> int forEach(File xml, String recordElement, Class<D> dtoClass, RecordHandler<D> handler)
            throws IOException, XMLStreamException {
        int read = 0;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(xml.toPath()), 1 << 16)) {
            XMLStreamReader reader = inputFactory().createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT
                            && recordElement.equals(reader.getLocalName())) {
                        D dto = xmlMapper.readValue(reader, dtoClass);
                        read++;
                        if (dto != null) {
                            handler.onRecord(dto);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        }
        return read;
    }

    private XMLInputFactory inputFactory() {
        return xmlMapper.getFactory().getXMLInputFactory();
    }
}

