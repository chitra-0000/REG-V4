package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.exporter.writer.GeneratedFileWriter;
import com.bnpp.regliss.exporter.writer.format.ExternalUrlFormatter;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.utils.FeedIdHelper;
import com.bnpp.regliss.vo.DJFeedId;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.defaultString;

@Slf4j
@ReglissBatchProfile
public class CoreXmlExportFormatWriter extends DJXmlExportFormatWriter {

    public CoreXmlExportFormatWriter(GeneratedFileWriter writer, String xmlHeader, ExternalUrlFormatter urlFormatter) {
        super(writer, xmlHeader, urlFormatter);
    }

    public void writeHeader(DJFileExportData exportParameters) {
        Version version = exportParameters.getVersion();
        DJFeedId nameDetails = FeedIdHelper.djFeedIdFromFilename(version.getFileName());
        fileWriter.write(xmlHeader);
        fileWriter.write("<DowJonesSanctionAlert date=\"" + nameDetails.getDateStamp() + "\" type=\""
                + fileWriter.getFileFormat().getFileExportMode().toExportTypeAttribute() + "\" >\n");
        fileWriter.write(defaultString(version.getVersionDJ().getReferentialsXmlPrefixSnippet()));
    }

    public void startRecords() { fileWriter.write("\n<Records>\n"); }

    public void endRecords() { fileWriter.write("</Records>\n"); }

    @Override
    public void startAssociations() {
        /*
         * Intentionally left empty as there are no associations for core exports.
         * */
    }

    @Override
    public void writeAssociationSnippet(DJAssociationXmlExportElementData associationsExportData, DJFileExportData djExportParameters) {
        //pretend to work (nothing to do here)
    }

    @Override
    public void endAssociations() {
        /*
         * Intentionally left empty as there are no associations for core exports.
         * */
    }

    public void writeFooter() { fileWriter.write("</DowJonesSanctionAlert>"); }

    @Override
    public void addAdditionalTagsValues(Map<ExportXmlTagType, List<String>> recordAdditionalTagsValues, DJFileExportData djExportParameters,
                                        Document doc) {
        addSource(recordAdditionalTagsValues, djExportParameters, doc);
    }

    private void addSource(Map<ExportXmlTagType, List<String>> recordAdditionalTagsValues, DJFileExportData djExportParameters, Document doc) {
        Element root = doc.getDocumentElement();
        String externalUUID = recordAdditionalTagsValues.get(ExportXmlTagType.SOURCE).stream().findFirst().orElseThrow(() ->
                new IllegalArgumentException("External UUID should be present"));
// [MISSING FROM SCREENSHOTS: lines 79-79 of the original file were not visible in the photos]
        NodeList sourceDetailsNodes = root.getElementsByTagName("SourceDetails");
        Element sourceDetails = doc.createElement("SourceDetails");

        Element sourceEn = doc.createElement("Source");
        Element sourceFr = doc.createElement("Source");

        Element refGroupeEn = doc.createElement("ReferenceGroup");
        refGroupeEn.setAttribute("ReferenceGroupCode", "REGLISS");
        Element refGroupeFr = doc.createElement("ReferenceGroup");
        refGroupeFr.setAttribute("ReferenceGroupCode", "REGLISS");
        sourceEn.appendChild(refGroupeEn);
        sourceFr.appendChild(refGroupeFr);

        Element sourceValueEn = doc.createElement("SourceValue");
        sourceValueEn.setAttribute("name", urlFormatter.getRecordUrl(djExportParameters.getListConfidentiality(), externalUUID,
                Locale.ENGLISH));

        Element sourceValueFr = doc.createElement("SourceValue");
        sourceValueFr.setAttribute("name", urlFormatter.getRecordUrl(djExportParameters.getListConfidentiality(), externalUUID,
                Locale.FRENCH));

        sourceEn.appendChild(sourceValueEn);
        sourceFr.appendChild(sourceValueFr);

        sourceDetails.appendChild(sourceEn);
        sourceDetails.appendChild(sourceFr);
        if (sourceDetailsNodes.getLength() == 0) {
            if (root.getElementsByTagName("ProfileNotes").getLength() > 0) {
                Node firstProfileNotesNode = root.getElementsByTagName("ProfileNotes").item(0);
                root.insertBefore(sourceDetails, firstProfileNotesNode);
            } else {
                root.appendChild(sourceDetails);
            }
        } else {
            sourceDetailsNodes.item(0).appendChild(sourceEn);
            sourceDetailsNodes.item(0).appendChild(sourceFr);
        }
    }

    @Override
    public void extraHandlerXmlSnippets(Document doc) {
        // No extra handle for Core
    }
}
