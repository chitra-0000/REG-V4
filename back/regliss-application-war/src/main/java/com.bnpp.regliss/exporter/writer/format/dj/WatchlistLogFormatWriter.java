package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.exporter.Description1FilterValues;
import com.bnpp.regliss.exporter.writer.FileGenerationTry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.BufferedWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
public class WatchlistLogFormatWriter {

    private WatchlistLogFormatWriter() {
        // do nothing
    }

    public static void generateLogFile(Map<String, FileGenerationTry> files, DJExportFilter filter, Set<Subscription> listSubscriptions,
                                       String logFileName, Version version, BufferedWriter writer) {
        log.info("GENERATION: WATCHLIST LOG");

        if (files.values().stream().anyMatch(FileGenerationTry::hasErrors)) {
            throw new IllegalStateException("Cannot writeRecords Watchlist ZIP file: some Watchlist files failed");
        }

        formatAndWrite(writer, "-------------------- DEBUT LOG REGLISS V4--------------------");
        formatAndWrite(writer, "Lancement du traitement");

        logSubscriberInfo(filter, listSubscriptions, writer);
        logFiltersInfo(filter, writer);

        formatAndWrite(writer, String.format("La liste %s.%s.%03d est complete.", version.getList().getShortName(),
                version.getList().getReference().replaceFirst("^0+", ""),
                version.getVersionNumber()));

        formatAndWrite(writer, "Creation d'un fichier ZIP");

        for (Map.Entry<String, FileGenerationTry> entry : files.entrySet()) {
            logFileAdditionToZip(entry.getKey(), entry.getValue().getFile().length(), writer);
        }
        formatAndWrite(writer, "Ajout du fichier " + logFileName);

        log.debug("DONE");
    }

    private static void logSubscriberInfo(DJExportFilter filter, Set<Subscription> listSubscriptions, BufferedWriter writer) {
        String subscriber = "";
        if (!CollectionUtils.isEmpty(listSubscriptions)) {
            subscriber = listSubscriptions.stream()
                    .map(Subscription::getSubscriber)
                    .filter(Objects::nonNull)
                    .map(Subscriber::getCftFluxName)
                    .collect(Collectors.joining(" "));
        }
        formatAndWrite(writer, "Nom du flux: " + subscriber);
        formatAndWrite(writer, "Type de fichier en entrée: " + filter.getFilterName());
        formatAndWrite(writer, "Commentaire du filtre :" + filter.getComment());
        formatAndWrite(writer, "Compression de fichier(s) a envoyer: .zip");
        formatAndWrite(writer, "La liste PEP est complète.");
    }

    private static void logFiltersInfo(DJExportFilter filter, BufferedWriter writer) {
        String description1 = "« Description1 =";
        String description2 = "« Description2 =";
        String description3 = "« Description3 =";
        String jurisdictionCountries = "« Country of jurisdiction =";
        String pepOcupation = "« Pep Occupations Categories =";
        String residenceCountries = "«  Country of residence =";
        String relationType = "«  Relationship =";
        String deathDatePepSip = "«  Deceased date as PEP/SIP=";
        String deathDateRCA = "«  Deceased date as RCA=";
        String inactivityDateAsPEP = "« Inactivity Date as PEP =";
        String inactivityDateAsRCA = "« Inactivity Date as RCA=";
        String genders = "« Genders=";
        if (!filter.exportsAll()) {
            for (DJExportSubfilter sf : filter.getActiveSubfilters()) {
                description1 = getDescription1(sf, description1);

                description3 = description3 + sf.getDescriptions3().stream().map(desc3 -> desc3.getSanctionType().getRecordCategory()
                                .getLabel())
                        .collect(Collectors.joining(",")) + ",";

                description2 = description2 + sf.getSanctionTypes().stream().map(st -> st.getLabel()).collect(Collectors.joining(","))
                        + ",";
                genders =genders+ sf.getGenders().stream().map(g->g.getLabel()).collect(Collectors.joining(",")) + ",";

                jurisdictionCountries = jurisdictionCountries + sf.getJurisdictionCountries().stream().map(country -> country.getLabel())
                        .collect(Collectors.joining(",")) + ",";
// [MISSING FROM SCREENSHOTS: lines 97-99 of the original file were not visible in the photos]
                        .collect(Collectors.joining(",")) + ",";
                relationType = relationType + sf.getRelationTypes().stream().map(rt -> rt.getLabel())
                        .collect(Collectors.joining(",")) + ",";
                if (sf.hasPepSipDeathDateCriteria()) {
                    deathDatePepSip = deathDatePepSip + sf.getDeathPepSipDateCriteria().getOperator().getValue() +
                            sf.getDeathPepSipDateCriteria().getYears();
                }
                if (sf.hasRCADeathDateCriteria()) {
                    deathDateRCA = deathDateRCA + sf.getDeathRCADateCriteria().getOperator().getValue() +
                            sf.getDeathRCADateCriteria().getYears();
                }
                if (sf.hasInactivityPepDateCriteria()) {
                    inactivityDateAsPEP = inactivityDateAsPEP + sf.getInactivityPepDateCriteria().getOperator().getValue() +
                            sf.getInactivityPepDateCriteria().getYears();
                }
                if (sf.hasInactivityRcaDateCriteria()) {
                    inactivityDateAsRCA = inactivityDateAsRCA + sf.getInactivityRcaDateCriteria().getOperator().getValue() +
                            sf.getInactivityRcaDateCriteria().getYears();
                }
            }

            formatAndWrite(writer, "Liste des filtre(s) appliqués: " + "\n"
                    + description1 + " »\n" +
                    description2 + " »\n" +
                    description3 + " »\n" +
                    pepOcupation + " »\n" +
                    residenceCountries + " »\n" +
                    jurisdictionCountries + " »\n" +
                    deathDatePepSip + " »\n" +
                    deathDateRCA + " »\n" +
                    inactivityDateAsPEP + " »\n" +
                    inactivityDateAsRCA + " »\n" +
                    genders + " »\n" +
                    relationType + " »");
        }
    }

    private static String getDescription1(DJExportSubfilter sf, String description1) {
        DJSubfilterDescription1 djSubfilterDescription1 = sf.getDescription1();
        if (!djSubfilterDescription1.isIncludeRca()) {
            description1 = description1 + Description1FilterValues.fromLabelEn(djSubfilterDescription1.
                    getRecordCategory().getLabel()) + ",";
        } else {
            description1 = description1 + Description1FilterValues.fromLabelEnAndIncludeRca(djSubfilterDescription1
                    .getRecordCategory().getLabel(), true, true) + ",";
        }
        return description1;
    }

    private static void logFileAdditionToZip(String zipEntryFilename, long fileLength, BufferedWriter writer) {
        formatAndWrite(writer, "Ajout du fichier " + zipEntryFilename
                + ", taille " + Math.round(fileLength / 1024.0));
    }

    @SneakyThrows
    private static void formatAndWrite(BufferedWriter bw, String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyy-HH:mm:ss");
        bw.write(formatter.format(LocalDateTime.now()) + " -|- " + message + "\n");
    }
}

