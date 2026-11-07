package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.batch.service.ExportPageType;
import com.bnpp.regliss.exporter.writer.GeneratedFileWriter;
import com.bnpp.regliss.exporter.writer.format.ExportFormatWriter;
import com.bnpp.regliss.exporter.writer.format.ExternalUrlFormatter;
import com.bnpp.regliss.exporter.writer.format.FileExportData;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.vo.export.ExportElementData;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import java.util.List;
import java.util.Map;
import static org.apache.commons.lang.StringUtils.defaultString;

@ReglissBatchProfile
@Slf4j
public abstract class DJXmlExportFormatWriter extends ExportFormatWriter {

    protected final ExternalUrlFormatter urlFormatter;

    protected final String xmlHeader;

    protected DJXmlExportFormatWriter(GeneratedFileWriter generatedFileWriter, String xmlHeader, ExternalUrlFormatter urlFormatter) {
        super(generatedFileWriter);
        this.xmlHeader = xmlHeader;
        this.urlFormatter = urlFormatter;
    }

    public abstract void extraHandlerXmlSnippets(Document doc);

    public abstract void addAdditionalTagsValues(Map<ExportXmlTagType, List<String>> recordAdditionalTagsValues,
                                                 DJFileExportData djExportParameters, Document doc);

    public abstract void writeHeader(DJFileExportData exportParameters);

    public abstract void startRecords();

    public abstract void endRecords();

    public abstract void startAssociations();

    public abstract void writeAssociationSnippet(DJAssociationXmlExportElementData associationsExportData,
                                                 DJFileExportData djExportParameters);

    public abstract void endAssociations();

    public abstract void writeFooter();

    @Override
    public void writeElement(ExportPageType pageType, ExportElementData dataForRecord, FileExportData fileExportData) {
        switch (pageType){
            case RECORDS_XML_SNIPPETS_PAGE:
                writeRecordXmlSnippet((DJXmlRecordExportElementData)dataForRecord, (DJFileExportData) fileExportData);
                break;
            case ASSOCIATIONS_XML_SNIPPETS_PAGE:
                writeAssociationSnippet((DJAssociationXmlExportElementData)dataForRecord, (DJFileExportData) fileExportData);
                break;
            default: throw new IllegalArgumentException("Page type not supported for DJ XML export: " + pageType);
        }
    }

    public void writeRecordXmlSnippet(DJXmlRecordExportElementData recordDjExportData, DJFileExportData djRecordsExportData) {
        Document doc = ExportDJXmlUtils.getXmlDocument(recordDjExportData.getRecordDjExportData().getExportSnippet());

        addAdditionalTagsValues(recordDjExportData.getAdditionalExportTagsValues(), djRecordsExportData, doc);

        this.extraHandlerXmlSnippets(doc);

        fileWriter.write(defaultString(ExportDJXmlUtils.getTransformedDocument(doc)));
        fileWriter.write("\n");
    }

}
