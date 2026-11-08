package com.bnpp.regliss.importer.dj;

// [MISSING FROM SCREENSHOTS: lines 3-27 of the original file were not visible in the photos]

@Slf4j

public class RecordXmlParser {
    private final Supplier<Transformer> transformerSupplier;
    private final WatchlistAssociationParser associationParser;
    private final AutomaticImportFeedBase feed;


    public RecordXmlParser(Supplier<Transformer> transformerSupplier, WatchlistAssociationParser associationParser, AutomaticImportFeedBase feed) {
        this.associationParser = associationParser;
        this.transformerSupplier = transformerSupplier;
        this.feed = feed;
    }

    public List<Record> parseXmlToRecords(List<RecordCumulatedXml> xmlSnippets, Set<DJXmlReferential> xmlRelationTypes) {
        log.info("Parsing {} updated xmlSnippets...", xmlSnippets.size());

        PerformanceMonitor.before(Action.XSLT);
        List<Record> records = xmlSnippets.stream()
                .map(r -> transformAndParseAsRecord(r, xmlRelationTypes))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
        PerformanceMonitor.after(Action.XSLT);
        return records;
    }

    public static void removeTransformerOnLocalThread() { TRANSFORMER_ON_THREAD.remove(); }

    private Optional<Record> transformAndParseAsRecord(RecordCumulatedXml xmlSnippet, Set<DJXmlReferential> xmlRelationTypes) {
        TransformedRecordSAXHandler handler = transformToIntermediateXmlSnippet(xmlSnippet);
        Optional<Record> record = handler.parseRecord();
        return record.map(r -> fillUnresolvedXsltFields(r, xmlSnippet, xmlRelationTypes));
    }

    private Record fillUnresolvedXsltFields(Record record, RecordCumulatedXml xmlSnippet, Set<DJXmlReferential> xmlRelationTypes) {
        record.setAssociations(xmlSnippet.getAssociationXml()
                .map(associationXml -> associationParser.parse(associationXml, xmlRelationTypes)).orElse(Collections.emptySet()));
        RecordDJ recordDj = new RecordDJ();
        recordDj.setXmlSnippet(xmlSnippet.getRecordXml());
        recordDj.setAssociationXml(xmlSnippet.getAssociationXml().orElse(null));
        record.setRecordDJ(recordDj);
        return record;
    }

    private TransformedRecordSAXHandler transformToIntermediateXmlSnippet(RecordCumulatedXml xmlSnippet) {
        TransformedRecordSAXHandler handler = new TransformedRecordSAXHandler();
        try {
            getTransformer().transform(new StreamSource(new StringReader(xmlSnippet.getRecordXml())), new SAXResult(handler));
        } catch (Exception e) {
            String transformedSnippet = tryWriteTransformedXmlToString(xmlSnippet.getRecordXml());
            log.error("Error applying XSLT transformation: {}\n Original xml snippet:\n {}\n Transformed xml:\n{}",
                    e, xmlSnippet,transformedSnippet);
            throw new ReglissException(e, "Could not transform original xml snippet. See logs for details");
        }
        return handler;
    }

    private String tryWriteTransformedXmlToString(String xmlSnippet) {
        try {
            log.info("Trying to serialize Transformed XML as string for exception reporting...");
            StringWriter stringWriter = new StringWriter();
            getTransformer().transform(new StreamSource(new StringReader(xmlSnippet)), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (Exception e) {
            log.warn("Could not even output the transformed xml as string", e);
            return "<Exception constructing transformed XML : " + e + ">";
        }
    }

    private static final ThreadLocal<Transformer> TRANSFORMER_ON_THREAD = new ThreadLocal<>();
    private Transformer getTransformer() {
        if(shouldReinitializeTransformer()) {
            Transformer transformer = transformerSupplier.get();
            transformer.setParameter("feedId", feed.getFeedId());
            TRANSFORMER_ON_THREAD.set(transformer);
        }
        return TRANSFORMER_ON_THREAD.get();
    }

    private boolean shouldReinitializeTransformer() {
        if(TRANSFORMER_ON_THREAD.get() == null) {
            log.info("Initializing transformer for new thread");
            return true;
        }
        boolean threadIsReusedAndProcessingDifferentFeed = !feed.getFeedId().equals(TRANSFORMER_ON_THREAD.get().getParameter("feedId"));
        if(threadIsReusedAndProcessingDifferentFeed){
            log.info("Initializing transformer for reused thread");
            return true;
        } else {
            log.debug("Skipping transformer initialization, using data from thread");
            return false;
        }
    }

}
