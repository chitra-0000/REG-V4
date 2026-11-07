package com.bnpp.regliss.exporter.writer.format.fircosoftxml;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.exporter.writer.FileGenerationTry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
public class FircosoftLogFormatWriter {

    private static final String MESSAGE = "Ajout du fichier ";

    private FircosoftLogFormatWriter() {
    }

    public static void generateLogFile(Version version, Map<String, FileGenerationTry> files, String logFileName, BufferedWriter writer) {
        log.info("GENERATION: FIRCOSOFT LOG");
        if (files.values().stream().anyMatch(FileGenerationTry::hasErrors)) {
            throw new IllegalStateException("Cannot writeRecords FIRCOSOFT ZIP file: some FIRCOSOFT files failed");
        }
        formatAndWrite(writer, "-------------------- DEBUT LOG REGLISS V4--------------------");
        formatAndWrite(writer, "Lancement du traitement");
        formatAndWrite(writer, "Repertoire racine: RESHUIS1");
        formatAndWrite(writer, "Type de fichier en entree: fircosoft");
        formatAndWrite(writer, "Compression de fichier(s) a envoyer: .zip");

        formatAndWrite(writer, String.format("La liste %s.%s.%03d est complete.", version.getList().getShortName(),
                version.getList().getReference().replaceFirst("^0+", ""),
                version.getVersionNumber()));

        formatAndWrite(writer, "Creation d'un fichier ZIP");

        for (Map.Entry<String, FileGenerationTry> entry : files.entrySet()) {
            logFileAdditionToZip(entry.getKey(), entry.getValue().getFile().length(), writer);
        }

        formatAndWrite(writer, MESSAGE + logFileName);
        log.debug("DONE");
    }

    public static void generateLogFileForConcatenatedList(Map<String, FileGenerationTry> files, String logFileName, BufferedWriter writer) {
        log.info("GENERATION: FIRCOSOFT LOG");
        if (files.values().stream().anyMatch(FileGenerationTry::hasErrors)) {
            throw new IllegalStateException("Cannot writeRecords FIRCOSOFT ZIP file: some FIRCOSOFT files failed");
        }
        formatAndWrite(writer, "-------------------- DEBUT LOG REGLISS V4--------------------");
        formatAndWrite(writer, "Lancement du traitement");
        formatAndWrite(writer, "Repertoire racine: RESHUIS1");
        formatAndWrite(writer, "Type de fichier en entree: fircosoft");
        formatAndWrite(writer, "Compression de fichier(s) a envoyer: .zip");

        formatAndWrite(writer, "Creation d'un fichier ZIP");

        for (Map.Entry<String, FileGenerationTry> entry : files.entrySet()) {
            logFileAdditionToZip(entry.getKey(), entry.getValue().getFile().length(), writer);
        }

        formatAndWrite(writer, MESSAGE + logFileName);
        log.debug("DONE");
    }

    private static void logFileAdditionToZip(String zipEntryFilename, long fileLength, BufferedWriter writer) {
        formatAndWrite(writer, MESSAGE + zipEntryFilename
                + ", taille " + Math.round(fileLength / 1024.0));
    }

    @SneakyThrows
    private static void formatAndWrite(BufferedWriter bw, String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyy-HH:mm:ss");
        bw.write(formatter.format(LocalDateTime.now()) + " -|- " + message + "\n");
    }

}

