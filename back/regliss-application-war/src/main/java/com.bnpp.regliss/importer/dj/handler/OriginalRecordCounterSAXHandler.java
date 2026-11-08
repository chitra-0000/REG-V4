package com.bnpp.regliss.importer.dj.handler;

// [MISSING FROM SCREENSHOTS: lines 3-15 of the original file were not visible in the photos]

@Slf4j
public class OriginalRecordCounterSAXHandler extends DefaultHandler {

    private long count;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("Entity".equals(qName) || "Person".equals(qName) || "record".equals(qName)) {
            count++;
        }
    }

    public long getCount() { return count; }

    public static long countRecords(Supplier<? extends InputStream> xmlInputStreamSupplier) {
        log.debug("Counting records...");
        OriginalRecordCounterSAXHandler handler = new OriginalRecordCounterSAXHandler();
        try (InputStream inputStream = xmlInputStreamSupplier.get())  {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            saxParser.parse(inputStream, handler);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new ReglissException(e);
        }
        log.debug("Total records in file: " + handler.getCount());
        return handler.getCount();
    }
}

