package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.DJLogFileCountType;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.importer.UnicodeBOMInputStream;
import com.bnpp.regliss.importer.dj.util.ClusteringIterator;
import com.bnpp.regliss.importer.dj.util.ClusteringIterator.LinesGroup;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.statistics.DjLogLivrasionStatistics;
import com.bnpp.regliss.utils.DJFilenameHelper;
import com.bnpp.regliss.utils.DjLogLivrasionUtil;
import com.bnpp.regliss.utils.FeedIdHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.bnpp.regliss.entity.ImportFileType.DOW_JONES_AME;
import static com.bnpp.regliss.entity.ImportFileType.DOW_JONES_WATCHLIST;
import static com.bnpp.regliss.utils.DJFilenameHelper.getWLAmeCsvPrefix;
import static com.bnpp.regliss.utils.DJFilenameHelper.getWLAmeXmlPrefix;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Service
@ReglissBatchProfile
@Slf4j
public class DJLogLivraisonService {

    @Autowired
    private AutomaticImportFileRepository fileRepo;

    @Autowired
    private DjLogLivrasionUtil djLogLivrasionUtil;

    public DjLogLivrasionStatistics getValueFromLog(File file){
        return this.djLogLivrasionUtil.extractRecordForToken(file);
    }

    public Map<DJLogFileCountType,Integer> countRecords(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier) {
        try {
            return tryCountRecords(xmlInputStreamSupplier);
        } catch (IOException e) {
            throw new ReglissException(e);
        }
    }

    private static class RecordCounterHandler extends DefaultHandler {
        @Getter
        private int totalRecords;
        @Getter
        private int totalAddedRecords;
        @Getter
        private int totalDeletedRecords;
        @Getter
        private int totalModifiedRecords;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (localName.equals("Person") || localName.equals("Entity")) {
                totalRecords ++;
                String valueOfAction = getActionValue(attributes);
                if(StringUtils.isNotEmpty(valueOfAction)) {
                    if(valueOfAction.equals("add")) {
                        this.totalAddedRecords++;
                    }
                    if(valueOfAction.equals("del")) {
                        this.totalDeletedRecords++;
                    }
                    if(valueOfAction.equals("chg")) {
                        this.totalModifiedRecords++;
                    }
                }

                if (totalRecords % 100_000 == 0) {
                    log.debug("{} records and counting...", totalRecords);
                }
            }
        }
        private String getActionValue(Attributes attributes) {
            String valueOfAction=attributes.getValue("recordaction");
            if(!StringUtils.isNotEmpty(valueOfAction)) {
                valueOfAction=attributes.getValue("action");
            }
            return valueOfAction;
        }
    }

    private EnumMap<DJLogFileCountType,Integer> tryCountRecords(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier) throws IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(xmlInputStreamSupplier.get()), 10*1024*1024)) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            RecordCounterHandler counter = new RecordCounterHandler();
            xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            xmlReader.setContentHandler(counter);
            xmlReader.parse(new InputSource(reader));

            EnumMap<DJLogFileCountType,Integer> result=new EnumMap<>(DJLogFileCountType.class);
            result.put(DJLogFileCountType.TOTAL,counter.getTotalRecords());
            result.put(DJLogFileCountType.ADD, counter.getTotalAddedRecords());
            result.put(DJLogFileCountType.DEL, counter.getTotalDeletedRecords());
            result.put(DJLogFileCountType.CHG, counter.getTotalModifiedRecords());
            log.debug("Counted {} records", result);
            return result;
        } catch (ParserConfigurationException | SAXException e) {
            throw new ReglissException(e);
        }
    }

    public synchronized void splitAnyLogsForFormat(ImportFileType importFileType, Map<String, ReglissList> allUpperCasePrefixes) {
        log.debug("Splitting logs for format: {}", importFileType);
        List<String> prefixesForFormat = getLogPrefixesForFormat(importFileType, allUpperCasePrefixes);
        fileRepo.getFilesInInputFolder().stream()
                .filter(DJFilenameHelper::isFullLog)
                .filter(filename -> hasOneOfPefixes(filename, prefixesForFormat))
                .map(fileRepo::getInputFileByName)
                .forEach(logFile -> splitLogFile(logFile, importFileType));
        log.debug("Splitted logs for format: {}", importFileType);
    }

    private List<String> getLogPrefixesForFormat(ImportFileType importFileType, Map<String, ReglissList> allUpperCasePrefixes) {
        switch (importFileType) {
            case DOW_JONES_WATCHLIST:
                return asList(getWLAmeXmlPrefix(DOW_JONES_WATCHLIST), getWLAmeCsvPrefix(DOW_JONES_WATCHLIST));

            case DOW_JONES_AME:
                return asList(getWLAmeXmlPrefix(DOW_JONES_AME), getWLAmeCsvPrefix(DOW_JONES_AME));
            case DOW_JONES_CORE:
                return allUpperCasePrefixes.entrySet().stream()
                        .filter(entry -> entry.getValue().getImportConfiguration().getImportFileType() == importFileType)
                        .map(Map.Entry::getKey).collect(toList());
            case DOW_JONES_WATCHLIST_EXTRA: return new ArrayList<String>();
            default:
                throw new IllegalArgumentException("Format " + importFileType + " does not allow log file.");
        }
    }

    private boolean hasOneOfPefixes(String filename, List<String> prefixesForFormat) {
        String upperCaseName = filename.toUpperCase();
        return prefixesForFormat.stream().anyMatch(prefixForFormat -> upperCaseName.startsWith(prefixForFormat.toUpperCase()));
    }

    public Optional<String> extractDelimiterLogFileName(String line) {
        if (StringUtils.isNotBlank(line) && line.contains("FEED FILE NAME")) {
            return Optional.of(line.substring(line.indexOf(":-") + 2).trim());
        } else {
            return Optional.empty();
        }
    }

    private void splitLogFile(File bigLogFile, ImportFileType importFileType) {
        try {
            List<String> splittedLogsFilenames = createSplitLogs(bigLogFile);
            log.info("Splitted {} log file: {} into files: {}", importFileType, bigLogFile, splittedLogsFilenames);
        } catch (Exception e) {
            log.warn("Could not split a file that appeared to be a log de livraison {}. Ignoring error.", bigLogFile);
            log.debug("Could not split a file that appeared to be a log de livraison {}.", bigLogFile, e);
        } finally {
            fileRepo.deleteInputFile(bigLogFile.getName());
            log.info("Finished splitting {} log file: {}", importFileType, bigLogFile);
        }
    }

    private List<String> createSplitLogs(File bigLogFile) throws IOException {
        try  (InputStream is = new FileInputStream(bigLogFile)) {
            List<String> logLines = IOUtils.readLines(is);
            return ClusteringIterator.breakByFirstLines(logLines.iterator(), this::extractDelimiterLogFileName)
                    .map(linesGroup -> createSplitLog(bigLogFile.getParentFile(), linesGroup))
                    .collect(Collectors.toList());
        }
    }

    private String createSplitLog(File folder, LinesGroup logLinesGroup) {
        String newLogFileName = StringUtils.EMPTY;
        String feedFileNameInLog = logLinesGroup.getKey();

        if (isNotBlank(feedFileNameInLog) && FeedIdHelper.filenameHasDJPattern(feedFileNameInLog)) {
            newLogFileName = feedFileNameInLog.substring(0, feedFileNameInLog.lastIndexOf('.')) + ".txt";
            try (OutputStream os = new FileOutputStream(new File(folder, newLogFileName))) {
                IOUtils.writeLines(logLinesGroup.getLines(), "\n", os);
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new ReglissException(e);
            }
            log.debug("Created split log file: " + newLogFileName);
        }
        return newLogFileName;
    }

}

