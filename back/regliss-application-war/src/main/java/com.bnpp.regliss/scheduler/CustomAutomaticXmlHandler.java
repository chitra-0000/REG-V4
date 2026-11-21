package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.importer.UnicodeBOMInputStream;
import lombok.Getter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.function.Supplier;

class CustomAutomaticXmlHandler extends DefaultHandler {

    @Getter
    private String sourceVersion;

    @Getter
    private String applicationDate;

    @Getter
    private String juridictionBase;

    private boolean isSourceVersion = false;
    private boolean isApplicationDate = false;
    private boolean isJuridictionBase = false;

    public static CustomAutomaticXmlHandler getXmlDataFromCustomAutomaticFile(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier) throws IOException {
        // TODO extract part to remove duplication in this class and DjLogLivraisonService
        try (Reader reader = new BufferedReader(new InputStreamReader(xmlInputStreamSupplier.get()), 10 * 1024 * 1024)) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            CustomAutomaticXmlHandler customAutomaticVersionHandler = new CustomAutomaticXmlHandler();
            xmlReader.setContentHandler(customAutomaticVersionHandler);
            xmlReader.parse(new InputSource(reader));

            return customAutomaticVersionHandler;
        } catch (ParserConfigurationException | SAXException e) {
            throw new ReglissException(e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("sourceVersion".equals(qName)) {
            this.isSourceVersion = true;
        } else if ("applicationDate".equals(qName)) {
            this.isApplicationDate = true;
        } else if ("juridictionBase".equals(qName)) {
            this.isJuridictionBase = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (this.isSourceVersion && ch.length > 0) {
            this.sourceVersion = new String(ch, start, length);
        } else if (this.isApplicationDate) {
            this.applicationDate = new String(ch, start, length);
        } else if (this.isJuridictionBase) {
            this.juridictionBase = new String(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        this.isSourceVersion = false;
        this.isApplicationDate = false;
        this.isJuridictionBase = false;
    }
}

