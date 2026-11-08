package com.bnpp.regliss.importer.dj.util;

// [MISSING FROM SCREENSHOTS: lines 3-13 of the original file were not visible in the photos]

/**
 * Iterates over XML Snippets of target element names
 */
public class XmlSnippetIterable implements Iterable<String>, Iterator<String>,AutoCloseable {
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();
    private XMLEventWriter writer = null;

    public enum ElementAction {
        INCLUDE, // will be returned by iterator
        SKIP,
        STOP // iteration should stop

    }
    private final XMLEventReader reader;
    private final InputStream inputStream;
    private final Function<String, ElementAction> getElementAction;
    private String nextSnippet;

    public XmlSnippetIterable(InputStream inputStream, Collection<String> elementNames) {
        this(inputStream, elementName -> elementNames.contains(elementName) ? ElementAction.INCLUDE : ElementAction.SKIP);
    }
    public XmlSnippetIterable(InputStream inputStream, Function<String, ElementAction> getElementAction) {
        this.inputStream = inputStream;
        this.getElementAction = getElementAction;
        try {
            reader = XML_INPUT_FACTORY.createXMLEventReader(inputStream);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Creating XML reader", e);
        }
    }

    @Override
    public Iterator<String> iterator() { return this; }

    @Override
    public boolean hasNext() {
        if (nextSnippet == null) {
            try {
                nextSnippet = extractNextSnippet();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }
        return nextSnippet != null;
    }

    /**
     * Returns the XML snippet of the next Record, if any
     */
    @Override
    public String next() {
        if (!hasNext()) {
            throw new IllegalStateException("No more snippets");
        }
        String result = nextSnippet;
        nextSnippet = null;
        return result;
    }

    private boolean shouldSplitOn(QName elementName) {
        return getElementAction.apply(elementName.getLocalPart()) == ElementAction.INCLUDE;
    }
    private boolean shouldStopOn(QName elementName) {
        return getElementAction.apply(elementName.getLocalPart()) == ElementAction.STOP;
    }

    private String extractNextSnippet() throws XMLStreamException {
        StringWriter xmlSnippetStringWriter = null;
        writer = null;
        String currentElement = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch(event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement startElement = (StartElement) event;
                    if (shouldStopOn(startElement.getName())) {
                        return null;
                    } else if (checkIfStartElement(currentElement, startElement)) {
                        currentElement = startElement.getName().getLocalPart();
                        xmlSnippetStringWriter = new StringWriter();
                        writer = XML_OUTPUT_FACTORY.createXMLEventWriter(xmlSnippetStringWriter);
                        writerNotNull(event);
                    } else {
                        writerNotNull(event);
                    }

                    break;

                case XMLStreamConstants.END_ELEMENT:
                    EndElement endElement = (EndElement)event;
                    if (endElement.getName().getLocalPart().equals(currentElement)) {
                        if (writer != null) {
                            writer.add(event);
                            writer.close();
                            return xmlSnippetStringWriter.toString();
                        }
                        this.writer = null;
                    } else {
                        writerNotNull(event);
                    }
                    break;

                default:
                    writerNotNull(event);
                    break;
            }
        }
        // no more snippets
        return null;
    }

    private boolean checkIfStartElement(String currentElement, StartElement startElement) {
        return currentElement == null && shouldSplitOn(startElement.getName());
    }
