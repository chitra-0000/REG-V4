package com.bnpp.regliss.utils;

import com.bnpp.regliss.entity.ImportFileType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.time.format.DateTimeFormatter;

@UtilityClass
@Slf4j
public class DJFilenameHelper {

    public static final DateTimeFormatter DJ_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final String WATCHLIST_TIME_SUFFIX = "2200";
    public static final String WATCHLIST_EXTRA_TIME_SUFFIX = "2359";
    private static String AME_TIME_SUFFIX = "2359";

    private static final String WATCHLIST_PREFIX = "pfa2";
    private static final String WATCHLIST_CSV_PREFIX = "csv_pfa";
    private final String WATCHLIST_EXTRA_CSV_PREFIX = "djrc_wl-ame_csv";
    public final String CHECKSUM_PREFIX = "checksum";
    private final String AME_PREFIX = "djrc_ame_xml";
    private final String AME_CSV_PREFIX = "djrc_ame_csv";

    public static boolean isXmlDataFile(String fileName) {
        return hasExtension(fileName, ".zip") || hasExtension(fileName, ".xml");
    }

    public static boolean isCsvDataFile(String fileName) {
        return hasExtension(fileName, ".zip") || hasExtension(fileName, ".csv");
    }

    public static boolean isSplittedDJLogFile(String fileName) {
        return isLogFile(fileName) && FeedIdHelper.filenameHasDJPattern(fileName);
    }

    public static boolean isFullLog(String fileName) {
        return isLogFile(fileName) && FeedIdHelper.filenameHasOriginalDJLogPattern(fileName);
    }

    public static boolean isWatchlistCsv(String fileName) {
        return fileName.toUpperCase().startsWith(WATCHLIST_CSV_PREFIX.toUpperCase())
                || fileName.toUpperCase().startsWith((CHECKSUM_PREFIX + "_" + WATCHLIST_CSV_PREFIX).toUpperCase());
    }

    public static boolean isAmeCsv(String fileName) {
        return fileName.toUpperCase().startsWith(AME_CSV_PREFIX.toUpperCase())
                || fileName.toUpperCase().startsWith((CHECKSUM_PREFIX + "_" + AME_CSV_PREFIX).toUpperCase());
    }

    public static boolean isAmeXml(String fileName) {
        return fileName.toUpperCase().startsWith(AME_PREFIX.toUpperCase())
                || fileName.toUpperCase().startsWith((CHECKSUM_PREFIX + "_" + AME_PREFIX).toUpperCase());
    }

    public static boolean isWatchlistXml(String fileName) {
        return fileName.toUpperCase().startsWith(WATCHLIST_PREFIX.toUpperCase())
                || fileName.toUpperCase().startsWith((CHECKSUM_PREFIX + "_" + WATCHLIST_PREFIX).toUpperCase());
    }

    public static boolean isChecksum(String fileName) {
        return fileName.toUpperCase().startsWith(CHECKSUM_PREFIX.toUpperCase());
    }

    public static String getWlAmeCsvPrefix(ImportFileType importFileType) {
        switch (importFileType) {
            case DOW_JONES_WATCHLIST:
                return DJFilenameHelper.WATCHLIST_CSV_PREFIX;
            case DOW_JONES_WATCHLIST_EXTRA:
                return DJFilenameHelper.WATCHLIST_EXTRA_CSV_PREFIX;
            case DOW_JONES_AME:
                return DJFilenameHelper.AME_CSV_PREFIX;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static String getWlAmeXmlPrefix(ImportFileType importFileType) {
        switch (importFileType) {
            case DOW_JONES_WATCHLIST:
                return DJFilenameHelper.WATCHLIST_PREFIX;
            case DOW_JONES_AME:
                return DJFilenameHelper.AME_PREFIX;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static String extractInterestingFileNamePart(String fileName) {
        String fileNamePart = FilenameUtils.getBaseName(fileName);
        if (DJFilenameHelper.isChecksum(fileNamePart)); {
            String checksumPrefix = CHECKSUM_PREFIX + "_";
            return fileNamePart.substring(checksumPrefix.length());
        }
        return fileNamePart;
    }

    public static boolean hasExtension(String fileName, String s) {
        return fileName.toUpperCase().endsWith(s.toUpperCase());
    }

    private static boolean isLogFile(String fileName) {
        return !isChecksum(fileName) && hasExtension(fileName, ".txt");
    }
}
