package com.bnpp.regliss.importer.dj.handler;

// [MISSING FROM SCREENSHOTS: lines 3-10 of the original file were not visible in the photos]

public class OriginalRecordDeletedSAXHandler extends DefaultHandler {

    private List<String> externalReferencesToDelete = new ArrayList<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("Person".equals(qName) || "Entity".equals(qName)) {
            if ("del".equals(attributes.getValue("recordaction")) ||
                    "del".equals(attributes.getValue("action"))) {
                String idAttribute = attributes.getValue("id");
                if (isNotBlank(idAttribute)) {
                    externalReferencesToDelete.add(idAttribute);
                }
            }
        }
    }

    public List<String> getExternalReferencesToDelete() { return externalReferencesToDelete; }
}

