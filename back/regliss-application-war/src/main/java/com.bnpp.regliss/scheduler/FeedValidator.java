package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ImportAutoPhase;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.importer.UnicodeBOMInputStream;
import com.bnpp.regliss.importer.dj.DJXsdValidator;
import com.bnpp.regliss.importer.manual.ManualImportFileRepository;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.VersionService;
import com.bnpp.regliss.statistics.DjLogLivrasionStatistics;
import com.bnpp.regliss.utils.TimeProvider;
import com.bnpp.regliss.vo.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@ReglissBatchProfile
@Slf4j
public class FeedValidator {

    @Value("${temp.file.folder}")
    private File baseTempFolder;

    @Autowired
    private AutomaticImportFileRepository automaticImportFileRepository;

    @Autowired
    private ManualImportFileRepository manualImportFileRepository;

    @Autowired
    private FileRepository fileRepo;

    @Autowired
    private UTF8InvalidFileNotifier utf8InvalidFileNotifier;

    @Autowired
    private AutomaticFeedService automaticFeedService;

    @Autowired
    private DJXsdValidator xsdValidator;

    @Autowired
    private DJLogLivraisonService logLivraisonService;

    @Autowired
    private BatchProgressService progressService;

    @Autowired
    private VersionService versionService;

    @Autowired
    private AutomaticSixImportFileRepository automaticSixImportFileRepository;

    private File tempFile;

    final static String UTF8_CHECK = "utf8check";
    final static String XML_EXTENSION = ".xml";
    final static String CSV_EXTENSION = ".csv";
    final static String[] ZIP_EXTENSIONS = {".zip",".ZIP"};
    final static String SEPARATOR = "/";

    public void verifyFeed(ReglissList list, AutomaticImportFeedBase feed, long batchExecutionId) {
        ImportConfiguration configuration = list.getImportConfiguration();
        if (feed.isDj()) {
            automaticFeedService.checkNotIncrementalAsFirstVersionInTx(list.getId(), (DJFeed) feed);

            if (configuration.isChecksumReception()) {
                verifyChecksum((DJFeed) feed);
            }
        }

        // a factory of InputStream, because we need to open the file multiple times
        Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier = openFromZipIfNecessary(feed);

        if (feed.isDj() && configuration.isLogReception()) {
            verifyXmlLogLivraison(xmlInputStreamSupplier, (DJFeed) feed);
            progressService.incrementImportAutoFeedValidatorBatchExecution(batchExecutionId, ImportAutoPhase.IMPORT_AUTO_VALIDATE_XML_LOG, configuration);
        }

        validateXsdSchema(xmlInputStreamSupplier, configuration.getImportFileType());

        if (feed.isCustomAutomatic() && feed instanceof CustomAutomaticFeed) { // instanceof check is done to remove compilation warning
            Optional<Version> versionOpt;
            if(feed.isAuto()){
                versionOpt =list.getLastVersion();
                versionOpt.ifPresent(version -> validateCurrentVersionGreaterThanPrevious(((CustomAutomaticFeed) feed).getFeedId(), version));
            } else {
                verifyEmptyFile(xmlInputStreamSupplier);
            }
        }

        progressService.incrementImportAutoFeedValidatorBatchExecution(batchExecutionId, ImportAutoPhase.IMPORT_AUTO_VALIDATE_XSD_SCHEMA, configuration);

        if (feed.isAuto() && automaticFeedService.feedCanOverwritePreviousVersionOfTheList(list, feed)) {
            Version previousVersion = list.getLastVersion().orElseThrow(() -> new ReglissException(ReglissException.ErrorCode.NOT_FOUND));
            log.debug("Will delete previous version id: {} for list id: {}. It will be overwritten by FULL feed from date: {}",
                    previousVersion.getId(), list.getId(), feed.getFeedId().getDateStamp());
            versionService.deleteVersionAndRecordsFromDBInTx(previousVersion.getId());
        }

        if (feed.isDj()) {
            verifyFutureDate(xmlInputStreamSupplier, (DJFeed) feed);
        }
    }

    public void checkUTF8InvalidFeedFiles(AutomaticImportFeedBase feed, ListIdAndVersionId listIdAndVersionId) {
        getFileNamesToValidateUTF8(feed).forEach(filename -> {
            try {
                sendMailIfNotValidUTF8File(feed, filename, listIdAndVersionId);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    protected void sendMailIfNotValidUTF8File(AutomaticImportFeedBase feed, String fileName, ListIdAndVersionId listIdAndVersionId) throws FileNotFoundException {
        Path path = getPathForFilename(feed, fileName);
        if (fileName.endsWith(ZIP_EXTENSIONS[0]) || fileName.endsWith(ZIP_EXTENSIONS[1])) {
            String unzipFileName = unzipToTempFile(path);
            path = Paths.get(unzipFileName);
        }
        Optional<String> utf8ErrorMessage = utf8InvalidFileNotifier.getUtf8ErrorMessage(path);
        if (null != tempFile) {
            tempFile.delete();
        }
        if (utf8ErrorMessage.isPresent()) {
            utf8InvalidFileNotifier.sendMailIfNotValidUTF8File(path, utf8ErrorMessage.get(), listIdAndVersionId);
            if (!feed.isAuto()) {
                throw new ReglissException(ReglissException.ErrorCode.DJ_IMPORT_VALIDATION_ERROR, "UTF8 Validation failed");
            }
        }
    }

    private String unzipToTempFile(Path path) throws FileNotFoundException {
        String  unzipPath = null;
        File zipFile  = new File(String.valueOf(path));
        try(ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))){
            ZipEntry entry;
            while((entry = zipInputStream.getNextEntry())!=null) {
                if (entry.getName().endsWith(XML_EXTENSION) || entry.getName().endsWith(CSV_EXTENSION)) {
                    tempFile = new File(baseTempFolder + SEPARATOR + UTF8_CHECK + XML_EXTENSION);
                    tempFile.createNewFile();
                    try (FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                    }
                    unzipPath = tempFile.getPath();
                }
            }

        } catch (IOException e) {
            throw new ReglissException(e);
        }
        return unzipPath;

    }

    public String dataStructureControl(AutomaticImportFeedBase feed) {
        Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier = openFromZipIfNecessary(feed);
        return getOrderingMarker(xmlInputStreamSupplier);
    }

    private void validateCurrentVersionGreaterThanPrevious(CustomAutomaticFeedId feedId, Version lastVersion) {
        Long currentVersionNumber = feedId.getSourceVersion();
        long lastVersionNumber = lastVersion.getVersionNumber();

        if (currentVersionNumber == null) {
            log.debug("Version number was not set in XML for {}", feedId);
        } else if (currentVersionNumber <= lastVersionNumber) {
            throw new ReglissException(ErrorCode.CA_APPLICATION_VERSION_TOO_OLD, Long.toString(currentVersionNumber));
        }
    }

    private void verifyXmlLogLivraison(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier, DJFeed feed) {
        File logFile = automaticImportFileRepository.getInputFileByName(feed.getXmlLogFileName());
        Map<DJLogFileCountType, Integer> resultsFromLogFile = new EnumMap<>(DJLogFileCountType.class);
        DjLogLivrasionStatistics djLogLivrasionStatistics = logLivraisonService.getValueFromLog(logFile);
        resultsFromLogFile.put(DJLogFileCountType.TOTAL, djLogLivrasionStatistics.getTotalRecords());
        resultsFromLogFile.put(DJLogFileCountType.ADD, djLogLivrasionStatistics.getAddRecords());
        resultsFromLogFile.put(DJLogFileCountType.DEL, djLogLivrasionStatistics.getSupRecords());
        resultsFromLogFile.put(DJLogFileCountType.CHG, djLogLivrasionStatistics.getModRecords());
        Map<DJLogFileCountType, Integer> resultsFromDJFile = logLivraisonService.countRecords(xmlInputStreamSupplier);

        if (!resultsFromDJFile.entrySet().stream().allMatch(e -> e.getValue().equals(resultsFromLogFile.get(e.getKey())))) {

            throw new ReglissException(ErrorCode.DJ_IMPORT_LOG_COUNT_INCORRECT, feed.getXmlLogFileName(), "     <p> Delivery File   Received file</p>" +
                    "<br/>Total   " + resultsFromLogFile.get(DJLogFileCountType.TOTAL) + "  " + resultsFromDJFile.get(DJLogFileCountType.TOTAL) +
                    "<br>ADD   " + resultsFromLogFile.get(DJLogFileCountType.ADD) + "  " + resultsFromDJFile.get(DJLogFileCountType.ADD) +
                    "<br/>CHG   " + resultsFromLogFile.get(DJLogFileCountType.CHG) + "  " + resultsFromDJFile.get(DJLogFileCountType.CHG) +
                    "<br/>SUPP   " + resultsFromLogFile.get(DJLogFileCountType.DEL) + "  " + resultsFromDJFile.get(DJLogFileCountType.DEL));
        }
        log.info("Log livraison verification OK. Total records: {}", feed);
    }

    private void verifyFutureDate(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier, DJFeed feed) {
        LocalDate dateInFile = getDateInFile(xmlInputStreamSupplier);
        if (ChronoUnit.DAYS.between(TimeProvider.now().toLocalDate(), dateInFile) > 30) {
            throw new ReglissException(ErrorCode.DJ_IMPORT_FUTURE_DATE, feed.getXmlLogFileName());
        }
        log.info("Future date verification OK. Number of days: {}", ChronoUnit.DAYS.between(LocalDate.now(), dateInFile));
    }

    private void verifyEmptyFile (Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier) {
        if(isEmptyFile(xmlInputStreamSupplier))
            throw new ReglissException(ErrorCode.CA_EMPTY_FILE);
    }

    private void verifyChecksum(DJFeed feed) {
        verifyChecksum(feed.getXmlDataFileName(), feed.getXmlChecksumFileName());
        if (feed.getCsvDataFileName() != null) {
            verifyChecksum(feed.getCsvDataFileName(), feed.getCsvChecksumFileName());
        }
        log.info("Checksum verification OK for feed {}", feed);
    }

    private void verifyChecksum(String dataFileName, String checksumFileName) {
        String valueOfFileA = automaticImportFileRepository.computeMD5HashOfInputFile(dataFileName);
        String valueInChecksumFile = automaticImportFileRepository.readInputFileFully(checksumFileName).trim();
        if (!valueOfFileA.equalsIgnoreCase(valueInChecksumFile)) {
            throw new ReglissException("Different checksum: "
                    + "Computed MD5 of file " + dataFileName + " = " + valueOfFileA
                    + ", different than value in checksum file " + checksumFileName + " = " + valueInChecksumFile + " ",
                    ErrorCode.DJ_IMPORT_CHECKSUM_INCORRECT);
        }
    }

    private void validateXsdSchema(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier, ImportFileType fileType) {
        log.info("Performing XSD Validation...");
        List<String> errors;
        try {
            errors = xsdValidator.validateDJXmlWithBom(xmlInputStreamSupplier, fileType);
        } catch (Exception e) {
            throw new ReglissException(e, ErrorCode.DJ_IMPORT_XSD_VALIDATION_FAILED, "XSD Validation failed: " + e.getMessage());
        }
        if (!errors.isEmpty()) {
            String errorsStr = String.join("\n", errors);
            log.info("XSD verification FAILED with {} errors", errors.size());
            throw new ReglissException(ErrorCode.DJ_IMPORT_XSD_VALIDATION_FAILED, errorsStr);
        }
        log.info("XSD verification PASSED");
    }

    private LocalDate getDateInFile(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier) {
        FutureDateHandler futureDateHandler = new FutureDateHandler();
        this.addHandlerToReader(futureDateHandler, xmlInputStreamSupplier);
        log.debug("Date in file: {}", futureDateHandler.getDateInFile());
        return futureDateHandler.getDateInFile();
    }

    private <T extends DefaultHandler> void addHandlerToReader(T handler, Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier)  {
        try (Reader reader = new BufferedReader(new InputStreamReader(xmlInputStreamSupplier.get()), 10 * 1024 * 1024)) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(reader));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ReglissException(e);
        }

    }

    private boolean isEmptyFile(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier) {
        EmptyFileHandler emptyFileHandler = new EmptyFileHandler();
        this.addHandlerToReader(emptyFileHandler, xmlInputStreamSupplier);
        log.debug("File is empty (doesn't have any record) : {}" , emptyFileHandler.isEmptyFile() );
        return emptyFileHandler.isEmptyFile();
    }

    protected List<String> getFileNamesToValidateUTF8(AutomaticImportFeedBase feed) {
        if (feed.isDj() && ((DJFeedId) (feed.getFeedId())).isWatchlist()) {
            return feed.getFileNamesToImport().stream()
                    .filter(s -> !s.toUpperCase().endsWith(".CSV"))
                    .collect(Collectors.toList());
        } else if (feed.isCustomAutomatic()){
            return feed.getFileNamesToImport().stream()
                    .filter(s -> !s.toUpperCase().endsWith(".ZIP"))
                    .collect(Collectors.toList());
        }else{
            return feed.getFileNamesToImport();
        }
    }

    private Path getPathForFilename(AutomaticImportFeedBase feed, String fileName) {
        if (feed.isAuto() && !feed.isSixAutomatic()) {
            return automaticImportFileRepository.getPathForFilename(fileName);
        } else if (feed.isAuto() && feed.isSixAutomatic()) {
            return automaticSixImportFileRepository.getPathForFilename(fileName);
        } else {
            return manualImportFileRepository.getPathForFilename(fileName);
        }
    }


    private String getOrderingMarker(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier) {
        OrderingMarkerHandler orderingMarkerHandler = new OrderingMarkerHandler();
        this.addHandlerToReader(orderingMarkerHandler,xmlInputStreamSupplier);
        log.debug("Date in file: {}", orderingMarkerHandler.getOrderingMarker());
        return orderingMarkerHandler.getOrderingMarker();
    }

    private static class FutureDateHandler extends DefaultHandler {

        @Getter
        private LocalDate dateInFile;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (localName.equals("PFA") || localName.equals("DowJonesSanctionAlert") || localName.equals("PFA2")) {
                String valueOfDate = attributes.getValue("date");
                if (StringUtils.isNotEmpty(valueOfDate)) {
                    //because we need to take only the first 8 characters from the string
                    final String datePart = valueOfDate.substring(0, 8);
                    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

                    dateInFile = LocalDate.parse(datePart, formatter);
                }
            }
        }
    }

    private static class EmptyFileHandler extends DefaultHandler {

        @Getter
        private boolean isEmptyFile = true;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (localName.equals("record")) {
                isEmptyFile =  false;
            }
        }
    }

    private static class OrderingMarkerHandler extends DefaultHandler {

        @Getter
        private String orderingMarker;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (localName.equals("PFA") || localName.equals("DowJonesSanctionAlert") || localName.equals("PFA2")) {
                orderingMarker = attributes.getValue("date");
            }
        }
    }

    private Supplier<UnicodeBOMInputStream> openFromZipIfNecessary(AutomaticImportFeedBase feed){
        if(feed.isAuto() && !feed.isSixAutomatic()){
            return automaticImportFileRepository.openFromZipIfNecessary(feed.getXmlDataFileName());
        } else if (feed.isAuto() && feed.isSixAutomatic()) {
            return automaticSixImportFileRepository.openFromZipIfNecessary(feed.getXmlDataFileName());
        } else{
            return manualImportFileRepository.openFromZipIfNecessary(feed.getXmlDataFileName());
        }
    }

}
