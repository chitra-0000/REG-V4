package com.bnpp.regliss.exporter.writer.format.dj;
// [MISSING FROM SCREENSHOTS: lines 2-24 of the original file were not visible in the photos]
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.defaultString;

@Slf4j
@ReglissBatchProfile
public class WatchlistXmlExportFormatWriter extends DJXmlExportFormatWriter {

    public WatchlistXmlExportFormatWriter(GeneratedFileWriter writer, String xmlHeader, ExternalUrlFormatter urlFormatter) {
        super(writer, xmlHeader, urlFormatter);
    }

    @Override
    public void writeHeader(DJFileExportData exportParameters) {
        DJWatchlistAmeFileExportData wlExportParameters = (DJWatchlistAmeFileExportData) exportParameters;
        DJFeedId nameDetails = FeedIdHelper.djFeedIdFromFilename(wlExportParameters.getVersion().getFileName());
        fileWriter.write(xmlHeader);
        fileWriter.write("<PFA date=\"" + nameDetails.getDateStamp() + "\" type=\""
                + fileWriter.getFileFormat().getFileExportMode().toExportTypeAttribute() + "\">\n");
        fileWriter.write(defaultString(wlExportParameters.getEnrichedReferentialXmlPrefix()));
    }

    @Override
    public void startRecords() { fileWriter.write("\n<Records>\n"); }

    @Override
    public void endRecords() { fileWriter.write("</Records>\n"); }

    @Override
    public void startAssociations() { fileWriter.write("<Associations>"); }

    @Override
    public void endAssociations() { fileWriter.write("</Associations>\n"); }

    @Override
    public void writeFooter() { fileWriter.write("</PFA>"); }

    public void writeAssociationSnippet(DJAssociationXmlExportElementData associationExportData, DJFileExportData djExportParameters) {
        long recordId = associationExportData.getRecordId();
        List<AssociationExternalReference> associationsToExport = ((DJWatchlistAmeFileExportData) djExportParameters)
                .getAssociationsForRecordAndFilter(recordId, getFilterId());
        if(associationsToExport != null) {
            writeXmlAssociationSnippet(associationExportData.getSnippet(), associationsToExport);
        }
    }

    private Long getFilterId() { return fileWriter.getFileFormat().getFilter().getId(); }

    private void writeXmlAssociationSnippet(String associationXmlSnippet, List<AssociationExternalReference> associationsToExport) {
        String header = associationXmlSnippet.substring(0, associationXmlSnippet.indexOf('>') + 1);
        String footer = associationXmlSnippet.substring(associationXmlSnippet.lastIndexOf('<'));
        try (XmlSnippetIterable iterable = new XmlSnippetIterable(new ByteArrayInputStream(associationXmlSnippet.getBytes()),
                singletonList("Associate"))) {
            List<String> filteredAssociations = filterAssociations(iterable, associationsToExport);
            if(!filteredAssociations.isEmpty()){
                List<String> associationsSnippetLines = new ArrayList<>();
                associationsSnippetLines.add(header);
                associationsSnippetLines.addAll(filteredAssociations);
                associationsSnippetLines.add(footer);
                writeXmlAssociationSnippet(String.join("\n", associationsSnippetLines));
            }
        }
    }

    private List<String> filterAssociations(XmlSnippetIterable actualAssociations, List<AssociationExternalReference> associationsToExport) {
        List<String> filteredAssociationSnippets = new ArrayList<>();
        for (String associateXml : actualAssociations) {
            AssociationExternalReference associationExternalReference = new AssociationExternalReference(extractIdFromXml(associateXml));
            if (associationsToExport.contains(associationExternalReference)) {
                filteredAssociationSnippets.add(associateXml);
            }
        }
        return filteredAssociationSnippets;
    }

    private String extractIdFromXml(String associateXml) {
        return XmlSnippetExternalReferenceExtractor.extractIdFromXml(associateXml, ImportFileType.DOW_JONES_WATCHLIST);
    }

    private void writeXmlAssociationSnippet(String xmlSnippet) {
        Document doc = ExportDJXmlUtils.getXmlDocument(xmlSnippet);
        fileWriter.write(defaultString(ExportDJXmlUtils.getTransformedDocument(doc)));
        fileWriter.write("\n");
    }

    @Override
    public void addAdditionalTagsValues(Map<ExportXmlTagType, List<String>> recordAdditionalTagsValues, DJFileExportData djExportParameters,
                                        Document doc) {
//      addSource(recordAdditionalTagsValues, djExportParameters, doc, root); this might be needed in the future,
//      the business said they might still need it
        addFiltersTags(recordAdditionalTagsValues, doc);
        addAddressTags( doc);
    }

    private void addAddressTags( Document doc) {
        WatchlistAmeAddressTagsRecordXmlEditor.addUnknownAddressCityTags( doc);
    }

    //this might be needed in the future, the business said they might still need it
    private void addSource(Map<ExportXmlTagType, List<String>> recordAdditionalTagsValues, DJFileExportParameters djExportParameters, Document doc) {
        Element root = doc.getDocumentElement();
        String externalUUID = recordAdditionalTagsValues.get(ExportXmlTagType.SOURCE).stream().findFirst().orElseThrow(()
                -> new IllegalArgumentException("External UUID should be present"));

        NodeList sourceDescNodeList = root.getElementsByTagName("SourceDescription");

        Element sourceEn = doc.createElement("Source");
        sourceEn.setAttribute("name", urlFormatter.getRecordUrl(djExportParameters.getListConfidentiality(), externalUUID,
                Locale.ENGLISH));

        Element sourceFr = doc.createElement("Source");
        sourceFr.setAttribute("name", urlFormatter.getRecordUrl(djExportParameters.getListConfidentiality(), externalUUID,
                Locale.FRENCH));

        Element sourceDescription = doc.createElement("SourceDescription");
        sourceDescription.appendChild(sourceFr);
        sourceDescription.appendChild(sourceEn);

        if (sourceDescNodeList.getLength() == 0) {
            boolean thereIsAnImagesNode = root.getElementsByTagName("Images").getLength() > 0;
            if (thereIsAnImagesNode) {
                Node firstImagesNode = root.getElementsByTagName("Images").item(0);
                root.insertBefore(sourceDescription, firstImagesNode);
            } else {
                root.appendChild(sourceDescription);
            }
        } else {
            sourceDescNodeList.item(0).appendChild(sourceFr);
            sourceDescNodeList.item(0).appendChild(sourceEn);
        }
    }

    private void addFiltersTags(Map<ExportXmlTagType, List<String>> recordTags, Document doc) {
        WatchlistAmeFiltersTagsRecordXmlEditor.addFiltersTags(recordTags, doc);
    }

    @Override
    public void extraHandlerXmlSnippets(Document doc) {
        Element root = doc.getDocumentElement();

        NodeList profileNotes = root.getElementsByTagName("ProfileNotes");

        if(profileNotes.getLength()>0){
            Node profileNote = profileNotes.item(0);
            String valueOfTheNode = profileNote.getTextContent();

            if(!StringUtils.isEmpty(valueOfTheNode) && valueOfTheNode.length()>2048){
                String newValueForNotes = valueOfTheNode.substring(0,2048);
                Node newProfileNode = profileNote.cloneNode(false);
                newProfileNode.setTextContent(newValueForNotes);
                root.replaceChild(newProfileNode,profileNote);

            }
        }
    }
}
