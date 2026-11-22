package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.ImportConfiguration;
import com.bnpp.regliss.utils.DJFilenameHelper;
import com.bnpp.regliss.utils.FeedIdHelper;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static java.util.stream.Collectors.toList;

@Getter
public class DJFeed implements AutomaticImportFeedBase {

    private final DJFeedId feedId;

    private Map<AutoFeedFileType, String> feedFileNames = new EnumMap<>(AutoFeedFileType.class);

    private boolean isAuto;

    private String originalFileName;

    private List<ImportLineErrorReport> errors = new ArrayList<>();

    public DJFeed(DJFeedId feedId) {
        this.feedId = feedId;
        this.isAuto = true;
    }

    public DJFeed(DJFeedId feedId, String filename) {
        this(feedId);
        fillFileName(filename);
    }

    public DJFeed(DJFeedId feedId, String filename, String originalFilename) {
        this(feedId);
        this.originalFileName = originalFilename;
        fillFileName(filename);
    }

    @Override
    public void addToErrors(List<ImportLineErrorReport> errors) {
        this.errors.addAll(errors);
    }

    @Override
    public void addToErrors(ImportLineErrorReport error) {
        this.errors.add(error);
    }

    @Override
    public List<String> getAllFileNames() {
        return feedFileNames.values().stream()
                .filter(Objects::nonNull).collect(toList());
    }

    @Override
    public String getXmlDataFileName() {
        return feedFileNames.get(AutoFeedFileType.XML_DATA_FILE);
    }

    @Override
    public String getOriginalFileName(){return originalFileName;}

    @Override
    public List<String> getFileNamesToImport() {
        List<String> fileNamesToImport = new ArrayList<>();
        fileNamesToImport.add(getXmlDataFileName());
        if (feedId.isWatchlist()) {
            fileNamesToImport.add(getCsvDataFileName());
        }
        return fileNamesToImport;
    }

    @Override
    public boolean isFullImport() {
        return getFeedId().getImportMode() == DJFileImportMode.FULL
                && !hasOnlyLogFiles();
    }

    @Override
    public final void fillFileName(String fileName) {
        if (DJFilenameHelper.isWatchlistCsv(fileName)) {
            fillCsvFilename(fileName);
        } else {
            fillXmlFilename(fileName);
        }
    }

    @Override
    public boolean hasAllFilesFor(ImportConfiguration configuration) {
        return getMissingFiles(configuration).isEmpty();
    }

    @Override
    public List<AutoFeedFileType> getMissingFiles(ImportConfiguration configuration) {
        return getRequiredFileTypes(configuration).stream()
                .filter(required -> StringUtils.isEmpty(feedFileNames.get(required)))
                .collect(toList());
    }

    @Override
    public boolean isDj() {
        return true;
    }

    @Override
    public boolean isCustomAutomatic() {
        return false;
    }

    @Override
    public boolean isSixAutomatic() {
        return false;
    }

    public Map<AutoFeedFileType, String> getAllFeedFiles() {
        return feedFileNames;
    }

    public boolean hasOnlyLogFiles() {
        boolean allFilesExceptLogsAreMissing = getXmlDataFileName() == null && getXmlChecksumFileName() == null
                        && getCsvDataFileName() == null && getCsvChecksumFileName() == null;
        boolean logFilesArePresent = getXmlLogFileName() != null;
        if(feedId.isWatchlist()){
            logFilesArePresent &= getCsvLogFileName() != null;
        }
        return allFilesExceptLogsAreMissing && logFilesArePresent;
    }

    public List<AutoFeedFileType> getRequiredFileTypes(ImportConfiguration configuration) {
        List<AutoFeedFileType> required = new ArrayList<>();
        if (getXmlDataFileName() == null) {
            required.add(AutoFeedFileType.XML_DATA_FILE);
        }
        if (configuration.isChecksumReception() && getXmlChecksumFileName() == null) {
            required.add(AutoFeedFileType.XML_CHECKSUM_FILE);
        }
        if (configuration.isLogReception() && getXmlLogFileName() == null) {
            required.add(AutoFeedFileType.XML_LOG_FILE);
        }
        if (configuration.isWatchlist() ) {
            if (getCsvDataFileName() == null) {
                required.add(AutoFeedFileType.CSV_DATA_FILE);
            }
            if (configuration.isChecksumReception() && getCsvChecksumFileName() == null) {
                required.add(AutoFeedFileType.CSV_CHECKSUM_FILE);
            }
            if (configuration.isLogReception() && getCsvLogFileName() == null) {
                required.add(AutoFeedFileType.CSV_LOG_FILE);
            }
        }
        return required;
    }

    public DJFeed setFileNameForType(AutoFeedFileType feedType, String fileName){
        feedFileNames.put(feedType, fileName);
        return this;
    }

    public DJFeed setXmlDataFileName(String xmlDataFileName) {
        this.feedFileNames.put(AutoFeedFileType.XML_DATA_FILE, xmlDataFileName);
        return this;
    }

    public String getXmlLogFileName() {
        return feedFileNames.get(AutoFeedFileType.XML_LOG_FILE);
    }

    public DJFeed setXmlLogFileName(String xmlLogFileName) {
        this.feedFileNames.put(AutoFeedFileType.XML_LOG_FILE, xmlLogFileName);
        return this;
    }

    public DJFeed setXmlChecksumFileName(String xmlChecksumFile) {
        this.feedFileNames.put(AutoFeedFileType.XML_CHECKSUM_FILE, xmlChecksumFile);
        return this;
    }

    @Override
    public DJFeedId getFeedId() {
        return feedId;
    }

    public String getXmlChecksumFileName() {
        return feedFileNames.get(AutoFeedFileType.XML_CHECKSUM_FILE);
    }

    public String getCsvDataFileName() {
        return feedFileNames.get(AutoFeedFileType.CSV_DATA_FILE);
    }

    public DJFeed setCsvDataFileName(String csvDataFileName) {
        this.feedFileNames.put(AutoFeedFileType.CSV_DATA_FILE, csvDataFileName);
        return this;
    }

    public String getCsvChecksumFileName() {
        return feedFileNames.get(AutoFeedFileType.CSV_CHECKSUM_FILE);
    }

    public DJFeed setCsvChecksumFileName(String csvChecksumFileName) {
        this.feedFileNames.put(AutoFeedFileType.CSV_CHECKSUM_FILE, csvChecksumFileName);
        return this;
    }

    public String getCsvLogFileName() {
        return feedFileNames.get(AutoFeedFileType.CSV_LOG_FILE);
    }

    public DJFeed setCsvLogFileName(String csvLogFileName) {
        this.feedFileNames.put(AutoFeedFileType.CSV_LOG_FILE, csvLogFileName);
        return this;
    }

    public static DJFeed fromDataFileName(String fileName) {
        return new DJFeed(FeedIdHelper.djFeedIdFromFilename(fileName), fileName);
    }

    private void fillCsvFilename(String fileName) {
        if (DJFilenameHelper.isChecksum(fileName)) {
            setCsvChecksumFileName(fileName);
        } else if (DJFilenameHelper.isSplittedDJLogFile(fileName)) {
            setCsvLogFileName(fileName);
        } else if (DJFilenameHelper.isCsvDataFile(fileName)) {
            setCsvDataFileName(fileName);
        } else {
            throw new IllegalArgumentException("Unknown file type");
        }
    }

    private void fillXmlFilename(String fileName) {
        if (DJFilenameHelper.isChecksum(fileName)) {
            setXmlChecksumFileName(fileName);
        } else if (DJFilenameHelper.isSplittedDJLogFile(fileName)) {
            setXmlLogFileName(fileName);
        } else if (DJFilenameHelper.isXmlDataFile(fileName)) {
            setXmlDataFileName(fileName);
        } else {
            throw new IllegalArgumentException("Unknown file type");
        }
    }

    @Override
    public void fillIsAuto(boolean flag) {
        isAuto = flag;
    }

    @Override
    public boolean isAuto() { return isAuto;}

    @Override
    public String toString() {
        List<String> fields = new ArrayList<>();
        if (getXmlDataFileName() != null) {
            fields.add("xml-data=" + getXmlDataFileName());
        }
        if (getXmlChecksumFileName() != null) {
            fields.add("xml-checksum=" + getXmlChecksumFileName());
        }
        if (getXmlLogFileName() != null) {
            fields.add("xml-log=" + getXmlLogFileName());
        }
        if (getCsvDataFileName() != null) {
            fields.add("csv-data=" + getCsvDataFileName());
        }
        if (getCsvChecksumFileName() != null) {
            fields.add("csv-checksum=" + getCsvChecksumFileName());
        }
        if (getCsvLogFileName() != null) {
            fields.add("csv-log=" + getCsvLogFileName());
        }
        return "{" + String.join(", ", fields) + "}";
    }

}
