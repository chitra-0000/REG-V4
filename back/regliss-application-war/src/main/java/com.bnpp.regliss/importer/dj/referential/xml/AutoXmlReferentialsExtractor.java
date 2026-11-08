package com.bnpp.regliss.importer.dj.referential.xml;

// [MISSING FROM SCREENSHOTS: lines 3-29 of the original file were not visible in the photos]

@Component
@ReglissBatchProfile
@Slf4j
public class AutoXmlReferentialsExtractor {
    private static final String DATE_TYPE_LIST ="DateTypeList";
    private static final String IDENTIFICATION_TYPE_LIST ="IdentificationTypeList";
    public DJXmlReferentials extractReferentialsUsedInParsing(Supplier<UnicodeBOMInputStream> xmlSupplier, ImportFileType format) {
        DJXmlReferentials referentials = new DJXmlReferentials();
        if(format.isCustomFileType()){
            return referentials;
        }
        log.debug("Extracting referentials from XML...");
        PerformanceMonitor.before(Action.EXTRACT_XML_REFERENTIALS_USED_IN_IMPORT);

        getFirstMatchUntilRecordsStart(xmlSupplier, "Description1List").ifPresent(xml ->
                referentials.set(DJReferentialType.RECORD_CATEGORY, tentativeParseAllChildren(xml,
                        node -> parseDescription1ReferentialBase(node, format))));

        getFirstMatchUntilRecordsStart(xmlSupplier, "Description2List").ifPresent(xml ->
                referentials.set(DJReferentialType.SANCTION_TYPE, tentativeParseAllChildren(xml,
                        node -> parseDescription2ReferentialBase(node, format))));

        getFirstMatchUntilRecordsStart(xmlSupplier, "Description3List").ifPresent(xml ->
                referentials.set(DJReferentialType.DESCRIPTION3, tentativeParseAllChildren(xml,
                        node -> parseDescription3ReferentialBase(node, format))));

        getFirstMatchUntilRecordsStart(xmlSupplier, "CountryList").ifPresent(xml ->
                {
                    referentials.set(DJReferentialType.COUNTRY, tentativeParseAllChildren(xml,
                            node -> getCountryCodeAttributeName(node, format)));
                    referentials.set(DJReferentialType.TERRITORY, tentativeParseAllChildren(xml,
                            node -> getISO2CountryCodeAttributeName(node, format)));
                });

        getFirstMatchUntilRecordsStart(xmlSupplier, "SanctionsReferencesList").ifPresent(xml ->
                referentials.set(DJReferentialType.SANCTION_REFERENCE, tentativeParseAllChildren(xml,
                        node -> parseDJReferential(node, format))));

        if (format.isCoreFormat()) {
            extractCoreReferentials(xmlSupplier, referentials);
        } else if (format.isWatchlistFormat() || format.isExtraWatchlistFormat()) {
            extractWatchlistReferentials(xmlSupplier, referentials);
        }

        PerformanceMonitor.after(Action.EXTRACT_XML_REFERENTIALS_USED_IN_IMPORT);

        log.debug("Extracted XML Referentials: {}", referentials);
        return referentials;
    }

    private void extractCoreReferentials(Supplier<UnicodeBOMInputStream> xmlSupplier, DJXmlReferentials djReferentialHolder) {
        getFirstMatchUntilRecordsStart(xmlSupplier,AutoXmlReferentialsExtractor.DATE_TYPE_LIST).ifPresent(xml ->
            djReferentialHolder.set(DJReferentialType.DATE_TYPE, tentativeParseAllChildren(xml, this::parseDJReferentialBase)));

        getFirstMatchUntilRecordsStart(xmlSupplier, "CountryTypeList").ifPresent(xml ->
            djReferentialHolder.set(DJReferentialType.COUNTRY_TYPE, tentativeParseAllChildren(xml, this::parseDJReferentialBase)));

        getFirstMatchUntilRecordsStart(xmlSupplier, AutoXmlReferentialsExtractor.IDENTIFICATION_TYPE_LIST).ifPresent(xml ->
            djReferentialHolder.set(DJReferentialType.IDENTITY_DOCUMENT_TYPE, tentativeParseAllChildren(xml, this::parseDJReferentialBase)));

        getFirstMatchUntilRecordsStart(xmlSupplier, AutoXmlReferentialsExtractor.IDENTIFICATION_TYPE_LIST).ifPresent(xml ->
            djReferentialHolder.set(DJReferentialType.PROGRAM_SANCTION_TYPE, tentativeParseAllChildren(xml, this::parseDJReferentialBase)));

        getFirstMatchUntilRecordsStart(xmlSupplier, "ScriptLanguageList").ifPresent(xml ->
            djReferentialHolder.set(DJReferentialType.LANGUAGE, tentativeParseAllChildren(xml, this::parseDJReferentialBase)));
    }

    private void extractWatchlistReferentials(Supplier<UnicodeBOMInputStream> xmlSupplier, DJXmlReferentials djReferentialHolder) {
        getFirstMatchUntilRecordsStart(xmlSupplier, "OccupationList").ifPresent(xml ->
// [MISSING FROM SCREENSHOTS: lines 100-102 of the original file were not visible in the photos]
            djReferentialHolder.set(DJReferentialType.RELATION_TYPE, tentativeParseAllChildren(xml,
                    this::parseRelationshipReferentialBase)));
        getFirstMatchUntilRecordsStart(xmlSupplier,AutoXmlReferentialsExtractor.DATE_TYPE_LIST).ifPresent(xml ->
            djReferentialHolder.set(DJReferentialType.DATE_TYPE, tentativeParseAllChildren(xml, this::parseDateTypeReferentialBase)));


    }


    public String extractAllReferentials(Supplier<UnicodeBOMInputStream> xmlSupplier) {
        return getAllMatchesUntilRecordsStart(xmlSupplier, createAllMatcher()).stream().collect(joining("\n"));
    }

    private Optional<String> getFirstMatchUntilRecordsStart(Supplier<? extends InputStream> xmlInputStreamSupplier, String referentialName) {
        Function<String, ElementAction> elementActionFunction = createSingleMatcher(referentialName);
        List<String> list = getAllMatchesUntilRecordsStart(xmlInputStreamSupplier, elementActionFunction);
        if (list.size() == 1) {
            return Optional.of(list.get(0));
        } else {
            return Optional.empty();
        }
    }

    /unchecked/
    private List<String> getAllMatchesUntilRecordsStart(Supplier<? extends InputStream> xmlInputStreamSupplier,
                                                        Function<String, ElementAction> elementActionFunction) {
        try (InputStream is = xmlInputStreamSupplier.get();
                XmlSnippetIterable iterable = new XmlSnippetIterable(is, elementActionFunction)) {
            return IteratorUtils.toList(iterable);
        } catch (IOException e) {
            throw new ReglissException(e);
        }
    }

    private Function<String, ElementAction> createSingleMatcher(String referentialName) {
        return elementName -> {
            if ("Records".equals(elementName) || "records".equals(elementName)) {
                return ElementAction.STOP;
            } else if (referentialName.equals(elementName)) {
                return ElementAction.INCLUDE;
            } else {
                return ElementAction.SKIP;
            }
        };
    }

    private Function<String,ElementAction> createAllMatcher() {
        return elementName -> {
            if ("Records".equals(elementName) || "records".equals(elementName)) {
                return ElementAction.STOP;
            } else if ("DowJonesSanctionAlert".equals(elementName) ||
                    "PFA".equals(elementName) || "list".equals(elementName)) {
                return ElementAction.SKIP;
            } else {
                return ElementAction.INCLUDE;
            }
        };
    }

    private Set<DJXmlReferential> tentativeParseAllChildren(String xml, Function<Node, Optional<DJXmlReferential>> childParser) {
        return getChildNodes(xml).stream()
                .map(childParser)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toSet());
    }

    private List<Node> getChildNodes(String xml) {
        if (StringUtils.isBlank(xml)) {
            return emptyList();
        }
        List<Node> list = new ArrayList<>();
        Document document = XmlUtils.parseXml(xml);
        NodeList childNodeList = document.getFirstChild().getChildNodes();
        for (int i = 0; i < childNodeList.getLength(); i++) {
            list.add(childNodeList.item(i));
        }
        return list;
    }

    private Optional<DJXmlReferential> parseDJReferential(Node node, ImportFileType format) {
        DJXmlReferentialWithReferenceGroupCode referential = new DJXmlReferentialWithReferenceGroupCode();
        referential.setCode(getAttributeValue(node, "code"));
        referential.setValue(getSanctionsReferenceCodeAttributeValue(node, format));
        referential.setReferenceGroupCode(getAttributeValue(node, "ReferenceGroupCode"));
        return Optional.of(referential);
    }

    private Optional<DJXmlReferential> parseDJReferentialBase(Node node) {
        DJXmlReferential referential = new DJXmlReferential();
        referential.setValue(node.getTextContent());
        referential.setCode(getAttributeValue(node, "code"));
        return Optional.of(referential);
    }

    private Optional<DJXmlReferential> parsePepOccupationReferentialBase(Node node) {
        DJXmlReferential referential = new DJXmlReferential();
        referential.setValue(getAttributeValue(node, "name"));
        referential.setCode(getAttributeValue(node, "code"));
        return Optional.of(referential);
    }

    private Optional<DJXmlReferential> parseRelationshipReferentialBase(Node node) {
        DJXmlReferential referential = new DJXmlReferential();
        referential.setValue(getAttributeValue(node, "name"));
        referential.setCode(getAttributeValue(node, "code"));
        return Optional.of(referential);
    }

    private Optional<DJXmlReferential> parseDateTypeReferentialBase(Node node) {
        DJXmlReferential referential = new DJXmlReferential();
        referential.setValue(getAttributeValue(node, "name"));
        referential.setCode(getAttributeValue(node, "name"));
        return Optional.of(referential);
    }


    private Optional<DJXmlReferential> parseDescription1ReferentialBase(Node node, ImportFileType format) {
        DJXmlReferential referential = new DJXmlReferential();
        referential.setValue(node.getTextContent());
        referential.setCode(getAttributeValue(node, getDescription1CodeAttributeName(format)));
        return Optional.of(referential);
    }

    private Optional<DJXmlReferential> parseDescription2ReferentialBase(Node node, ImportFileType format) {
        DJXmlReferentialWithParentCode referential = new DJXmlReferentialWithParentCode();
        referential.setValue(node.getTextContent());
        referential.setCode(getAttributeValue(node, getDescription2CodeAttributeName(format)));
        referential.setParentCode(getAttributeValue(node, getDescription1CodeAttributeName(format)));
        return Optional.of(referential);
    }

    private Optional<DJXmlReferential> parseDescription3ReferentialBase(Node node, ImportFileType format) {
        DJXmlReferentialWithParentCode referential = new DJXmlReferentialWithParentCode();
        referential.setValue(node.getTextContent());
        referential.setCode(getAttributeValue(node, getDescription3CodeAttributeName(format)));
        referential.setParentCode(getAttributeValue(node, getDescription2CodeAttributeName(format)));
        return Optional.of(referential);
    }

    private String getAttributeValue(Node node, String attributeName) {
        NamedNodeMap nodeAttributes = node.getAttributes();
        if (nodeAttributes != null) {
            Node codeNode = nodeAttributes.getNamedItem(attributeName);
            if (codeNode != null) {
                return ((Attr) codeNode).getValue();
            }
        }
        return null;
    }

    private String getSanctionsReferenceCodeAttributeValue(Node node, ImportFileType format) {
        if(format == ImportFileType.DOW_JONES_CORE) {
            return node.getTextContent();
        } else {
            return getAttributeValue(node, "name");
        }
    }

    private String getDescription1CodeAttributeName(ImportFileType format) {
        if(format == ImportFileType.DOW_JONES_CORE) {
            return "Description1Code";
        } else {
            return "Description1Id";
        }
    }

    private String getDescription2CodeAttributeName(ImportFileType format) {
        if(format == ImportFileType.DOW_JONES_CORE) {
            return "Description2Code";
        } else {
            return "Description2Id";
        }
    }

    private String getDescription3CodeAttributeName(ImportFileType format) {
        if(format == ImportFileType.DOW_JONES_CORE) {
            return "Description3Code";
        } else {
            return "Description3Id";
        }
    }

    private Optional<DJXmlReferential> getCountryCodeAttributeName(Node node, ImportFileType format) {
        DJXmlReferential referential = new DJXmlReferential();
        referential.setCode(getAttributeValue(node, "code"));
        if(format == ImportFileType.DOW_JONES_CORE) {
            referential.setValue(node.getTextContent());
        } else {
            referential.setValue(getAttributeValue(node, "name"));
        }
        return Optional.of(referential);
    }

    private Optional<DJXmlReferential> getISO2CountryCodeAttributeName(Node node, ImportFileType format) {
        DJXmlReferential referential = new DJXmlReferential();
        referential.setCode(getAttributeValue(node, "ISO2CountryCode"));
        if(format == ImportFileType.DOW_JONES_CORE) {
            referential.setValue(node.getTextContent());
        } else {
            referential.setValue(getAttributeValue(node, "name"));
        }
        return Optional.of(referential);
    }

}
