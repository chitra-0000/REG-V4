package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.ReglissException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.lang.StringUtils.defaultString;

@Slf4j
@UtilityClass
public class ExportDJXmlUtils {

    private static final String FICTIONAL_ROOT_START_TAG = "<technicalRoot>";
    private static final String FICTIONAL_ROOT_END_TAG = FICTIONAL_ROOT_START_TAG.replaceFirst("<", "</");

    public Document getXmlDocument(String xmlSnippet) {
        ByteArrayInputStream input = new ByteArrayInputStream(
                defaultString(xmlSnippet).getBytes(StandardCharsets.UTF_8));
        DocumentBuilder docBuilder;
        Document doc;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            docBuilder = dbf.newDocumentBuilder();
            doc = docBuilder.parse(input);
        } catch (Exception e) {
            log.debug("Error while parsing the xmlSnippet from RecordDJ");
            throw new ReglissException(e);
        }
        return doc;
    }

    public String getTransformedDocument(Document doc) {
        String xmlOutput;
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            transformerFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
            transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource domSource = new DOMSource(doc);
            transformer.transform(domSource, result);
            xmlOutput = result.getWriter().toString();
        } catch (Exception e) {
            throw new ReglissException(e);
        }
        return xmlOutput;
    }

    //Needed in case of snippets that are not valid xml document (e.g. referential snippet from db)
    public static Document getDocumentWithFictionalRoot(String xmlSnippet) {
        String initialXmlReferential = FICTIONAL_ROOT_START_TAG + xmlSnippet  + FICTIONAL_ROOT_END_TAG;
        return ExportDJXmlUtils.getXmlDocument(initialXmlReferential);
    }

    public static String removeFictionalRoot(Document referentialDocument) {
        String transformedDocument = getTransformedDocument(referentialDocument);
        transformedDocument = transformedDocument.replaceFirst(FICTIONAL_ROOT_START_TAG, "");
        transformedDocument = transformedDocument.substring(0, transformedDocument.lastIndexOf(FICTIONAL_ROOT_END_TAG));
        return transformedDocument;
    }
}
