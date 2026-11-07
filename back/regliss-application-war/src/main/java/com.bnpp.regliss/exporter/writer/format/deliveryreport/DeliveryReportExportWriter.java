package com.bnpp.regliss.exporter.writer.format.deliveryreport;

import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.batch.service.ExportPhase;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.ListCategory;
import com.bnpp.regliss.entity.referential.Scope;
import com.bnpp.regliss.exporter.writer.GeneratedFileWriter;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.comparing;

@Service
@Slf4j
@ReglissBatchProfile
public class DeliveryReportExportWriter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private BatchProgressService progressService;

    public DeliveryReportExportWriter(BatchProgressService progressService) { this.progressService = progressService; }

    public void generateDeliveryReport(Set<ReglissList> listsToBeIncluded, GeneratedFileWriter file, ExportJobParameters exportJobParameters) {
        log.info("GENERATION: DELIVERY REPORT");
        writeHeader(file);

        for (ReglissList list : listsToBeIncluded) {
            writeReglissListSubscription(list, file);
        }
        writeFooter(file);

        progressService.incrementBatchExportProgressForWritingAllPagesAtOnceOrNoPages(ExportPhase.EXPORT_WRITE_RECORDS, 1,
                exportJobParameters);
        log.debug("DONE");
    }

    private void writeHeader(GeneratedFileWriter file) {
        file.addCell("<?xml version = \"1.0\" encoding = \"UTF-8\" standalone= \"yes\"?>");
        file.writeLine();
        file.addCell("<groupList>");
        file.writeLine();
    }

    private void writeFooter(GeneratedFileWriter file) {
        file.addCell("</groupList>");
        file.writeLine();
    }

    private void writeReglissListSubscription(ReglissList list, GeneratedFileWriter file) {
        StringBuilder content = new StringBuilder();
        Optional<Version> lastPublishedVersion = getLastPublishedVersion(list );
        content.append("<list>").append("\n")
            .append("<name>").append(list.getName()).append("</name>").append("\n")
            .append("<shortName>").append(list.getShortName()).append("</shortName>").append("\n")
            .append("<type>").append(list.getType()).append("</type>").append("\n")
            .append("<kind>").append(kindToString(list.getListCategory())).append("</kind>").append("\n")
            .append("<impact>").append(Scope.GROUP.equals(list.getScope()) ? "Groupe" : "Locale").append("</impact>").append("\n")
            .append("<confidentiality>").append(confidentialityToString(list.getConfidentiality())).append("</confidentiality>").append("\n")
            .append("<version>").append(lastPublishedVersion.isPresent() ? lastPublishedVersion.get().getVersionNumber() : "")
                .append("</version>").append("\n")
            .append("<versionDate>").append(lastPublishedVersion.isPresent() ? lastPublishedVersion.get().getCreationDate()
                        .format(DATE_TIME_FORMATTER) : "").append("</versionDate>").append("\n")
            .append("<publicationDate>").append(getPublicationDateString(lastPublishedVersion)).append("</publicationDate>").append("\n")
            .append("<vigilanceReference>").append(list.getReference()).append("</vigilanceReference>").append("\n")
            .append("<territory>").append((list.getCountry() != null) ? list.getCountry().getISO2Code() : null).append("</territory>")
                .append("\n")
            .append("<mandatory>").append("Obligatoire").append("</mandatory>").append("\n")
            .append("</list>").append("\n");

        file.addCell(content.toString());
    }
    private Optional<Version> getLastPublishedVersion(ReglissList list) {
        return list.getVersions().stream()
                .filter(Version::isNotDeleted)
                .filter(Version::wasCertified)
                .filter(Version::wasPublished)
                .max(comparing(Version::getVersionNumber));
    }
    private String getPublicationDateString(Optional<Version> version) {
        return version.map(v->v.getPublicationDate().format(DATE_TIME_FORMATTER)).orElse("");
    }
    private Object kindToString(ListCategory c) {
        switch (c.getLabel()) {
            case "Sanctions":
                return "SANCTIONS";
            case "AML/CTF":
                return "AML_CTF";
            case "Non Financial Security":
                return "NON_FINANCIAL_SECURITY";
            default:
                throw new IllegalArgumentException(c.getLabel());
        }
    }

    private Object confidentialityToString(Confidentiality c) {
        switch (c) {
            case CONFIDENTIAL:
                return "Confidentielle";
            case RESTRICTED:
                return "Restreinte";
            case PUBLIC:
                return "Publique";
            default:
                throw new IllegalArgumentException(c.name());
        }
    }
}

