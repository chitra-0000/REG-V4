package com.bnpp.regliss.importer.dj;
// [MISSING FROM SCREENSHOTS: lines 2-53 of the original file were not visible in the photos]
    }

    public static String pretty(String xmlString)  {
        if (xmlString == null) {
            return "";
        }
        return pretty(parseXml(xmlString));
    }

    public static String pretty(Node xml)  {
        if (xml == null) {
            return "";
        }
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            //initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(xml);
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (TransformerException e) {
            throw canNotPrettyTransform(xml, e);
        }
    }

    public  static Document parseXml(String xmlString) {
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();//NOSONAR properties set in factory
            InputSource is = new InputSource(new StringReader(xmlString));
            return db.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw canNotPrettyTransform(xmlString, e);
        }
    }


    public static String formatXml(String unformatted) throws TransformerException, ParserConfigurationException, IOException,
            SAXException, XPathExpressionException {
        if (StringUtils.isBlank(unformatted)) {
            return StringUtils.EMPTY;
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(unformatted)));

        normalizeDoc(doc);
        Transformer transformer = formattingTransformer();
        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(new DOMSource(doc), result);
        return result.getWriter().toString();
    }

    private static Transformer formattingTransformer() throws TransformerConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        transformerFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
        transformerFactory.setAttribute("indent-number", 2);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        return transformer;
    }

    private static void normalizeDoc(Document doc) throws XPathExpressionException {
        doc.normalize();
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", doc, XPathConstants.NODESET);
        for (int i = 0; i <nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            node.getParentNode().removeChild(node);
        }
    }

    public static String getNodeXml(Node node) {
        try {
            DOMSource source = new DOMSource(node);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter sw = new StringWriter();
            StreamResult sr = new StreamResult(sw);
            transformer.transform(source, sr);
            return sw.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException(format("Could not get node: <%s>", node), e);
        }
    }

    public static Optional<Node> getOneChild(Node node, String childName) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            if (child.getNodeName().equals(childName)) {
                return of(child);
            }
        }
        return empty();
    }

    public static List<Node> getChildren(Node node, String childName) {
        List<Node> children = new ArrayList<>();
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            if (child.getNodeName().equals(childName)) {
                children.add(child);
            }
        }
        return children;
    }
    private static IllegalStateException canNotPrettyTransform(Object xml, Exception e) {
        return new IllegalStateException(format("Can not pretty print <%s>", xml), e);
    }
}

