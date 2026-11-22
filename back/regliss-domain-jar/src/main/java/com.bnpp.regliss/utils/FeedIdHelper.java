package com.bnpp.regliss.utils;

import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.vo.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

@UtilityClass
@Slf4j
public class FeedIdHelper {

    public static final Pattern CA_PATTERN = Pattern.compile("([^.]+)\\.(.+)");
    public static final Pattern DJ_PATTERN = Pattern.compile("(.+)_([0-9]{8,})_([iIfF])");
    public static final Pattern DJ_LOG_PATTERN = Pattern.compile("(.+)_([0-9]{8,})");
    public static final Pattern SGR_PATTERN = Pattern.compile("(.+)_([0-9]{8,})_([0-9]{4,})");
    public static final Pattern MLM_PATTERN = Pattern.compile("(.+)_([0-9]{8,})_([0-9]{6,})");
    public static final Pattern SIX_RAW_PATTERN = Pattern.compile("(.+)_([0-9]{8,})_([0-9]{6,})");
    private static final Pattern SIX_PATTERN = Pattern.compile("^([^_]+)_([0-9]+)_([0-9]{8})_([0-9]{6})" + "(?:\\.xml)?$");

    public static boolean filenameHasCorrectPatternForFormat(String filename, ImportFileType importFormat) {
        if (importFormat.isDjFormat()) {
            return filenameHasDJPattern(filename);
        } else if (importFormat.isSixRawFormat()) {
            return filenameHasSIXPattern(filename);
        } else if (importFormat.isSIXMainFileType()) {
            return filenameHasSIXMainFilePattern(filename);
        }else {
            return filenameHasCAPattern(filename);
        }
    }

    public static AutomaticImportFeedIdBase feedIdFromFilename(String filename, ImportFileType importFormat) {
        if (importFormat.isDjFormat()) {
            return djFeedIdFromFilename(filename);
        } else if (importFormat.isSixRawFormat()) {
            return sixFeedIdFromFileName(filename);
        } else if (importFormat.isSIXMainFileType()) {
            return sixFeedIdFromMainFileName(filename);
        }else {
            return caFeedIdFromFilename(filename);
        }
    }

    public static CustomAutomaticFeedId caFeedIdFromFilename(String filename) {
        String nameWithoutExtension = FilenameUtils.getBaseName(filename).toUpperCase();
        Matcher m ;
        if(filename.startsWith("bnl"))
        {

            m = SGR_PATTERN.matcher(nameWithoutExtension);
        }
        else if(filename.startsWith("mlm")){
            m = MLM_PATTERN.matcher(nameWithoutExtension);
        }
        else{
            m = CA_PATTERN.matcher(nameWithoutExtension);
        }

        if (!m.matches()) {
            throw new IllegalArgumentException(format("Custom Automatic file name '%s' does not match expected pattern: '%s'", nameWithoutExtension, CA_PATTERN.pattern()));
        }
        String prefix = m.group(1);
        String orderingMarker = m.group(2);
        if(filename.startsWith("mlm"))
        {
            prefix = m.group(1).substring(0,m.group(1).lastIndexOf("_"));
            orderingMarker = m.group(2) + m.group(3);
        }
        return new CustomAutomaticFeedId(prefix, orderingMarker);
    }

    // "S_XML_1038_SERVICEID_20171031_1_f"
    public static DJFeedId djFeedIdFromFilename(String fileName) {
        String fileNamePart = DJFilenameHelper.extractInterestingFileNamePart(fileName);
        Matcher m = DJ_PATTERN.matcher(fileNamePart);
        if (!m.matches()) {
            throw new IllegalArgumentException(format("DJ filename '%s' does not match expected pattern: '%s'", fileNamePart, DJ_PATTERN.pattern()));
        }
        String prefix = m.group(1);
        String dateStamp = m.group(2);
        DJFileImportMode importMode = DJFileImportMode.fromImportFileNameSuffix(m.group(3));
        return new DJFeedId(prefix, dateStamp, importMode);
    }

    public static SixAutomaticFeedId sixFeedIdFromFileName(String fileName) {
        String nameWithoutExtension = FilenameUtils.getBaseName(fileName).toUpperCase();
        Matcher m = SIX_RAW_PATTERN.matcher(nameWithoutExtension);
        if (!m.matches()) {
            throw new IllegalArgumentException(format("SIX filename '%s' does not match expected pattern: '%s'", nameWithoutExtension, SIX_RAW_PATTERN.pattern()));
        }

        String prefix = m.group(1);
        String dateStamp = m.group(2) + m.group(3);
        return new SixAutomaticFeedId(prefix, dateStamp);
    }

    public static CustomAutomaticFeedId sixFeedIdFromMainFileName(String fileName) {
        String nameWithoutExtension = FilenameUtils.getBaseName(fileName).toUpperCase();
        Matcher m = SIX_PATTERN.matcher(nameWithoutExtension);
        if (!m.matches()) {
            throw new IllegalArgumentException(format("SIX filename '%s' does not match expected pattern: '%s'", nameWithoutExtension, SIX_RAW_PATTERN.pattern()));
        }
        String prefix = m.group(1);
        String dateStamp = m.group(3) + m.group(4);
        return new CustomAutomaticFeedId(prefix, dateStamp);
    }

    // "S_XML_1038_SERVICEID_20171031_1_f"
    public static LocalDate djDateFromWlFilename(String fileName) {

        String fileNamePart = DJFilenameHelper.extractInterestingFileNamePart(fileName);
        Matcher m = DJ_PATTERN.matcher(fileNamePart);
        if (!m.matches()) {
            throw new IllegalArgumentException(format("DJ filename '%s' does not match expected pattern: '%s'", fileNamePart, DJ_PATTERN.pattern()));
        }
        String dateStamp = m.group(2);
        return parseStartDate(dateStamp);
    }


    public static boolean filenameHasCAPattern(String fileName) {
        String nameWithoutExtension = FilenameUtils.getBaseName(fileName).toUpperCase();
        if(fileName.startsWith("bnl")) {
            return filenameHasPattern(nameWithoutExtension, SGR_PATTERN);
        } else if(fileName.startsWith("mlm")) {
            return filenameHasPattern(nameWithoutExtension, MLM_PATTERN);
        }
        else {
            return filenameHasPattern(nameWithoutExtension, CA_PATTERN);
        }
    }

    public static boolean filenameHasDJPattern(String fileName) {
        return filenameHasPattern(DJFilenameHelper.extractInterestingFileNamePart(fileName), DJ_PATTERN);
    }

    public static boolean filenameHasSIXPattern(String fileName) {
        String nameWithoutExtension = FilenameUtils.getBaseName(fileName).toUpperCase();
        return filenameHasPattern(nameWithoutExtension, SIX_RAW_PATTERN);
    }

    public static boolean filenameHasSIXMainFilePattern(String fileName) {
        String nameWithoutExtension = FilenameUtils.getBaseName(fileName).toUpperCase();
        return filenameHasPattern(nameWithoutExtension, SIX_PATTERN);
    }

    public static boolean filenameHasOriginalDJLogPattern(String fileName) {
        return filenameHasPattern(DJFilenameHelper.extractInterestingFileNamePart(fileName), DJ_LOG_PATTERN);
    }

    public LocalDate parseStartDate(AutomaticImportFeedIdBase feedId) { return parseStartDate(feedId.getDateStamp()); }
    public LocalDate parseStartDate(String feedDate){
        return LocalDate.parse(feedDate.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
    }

    private static boolean filenameHasPattern(String fileName, Pattern pattern) {
        try {
            Matcher m = pattern.matcher(fileName);
            return m.matches();
        } catch (Exception e) {
            log.debug("Filename does not match expected pattern: '{}': ", fileName, e);
            return false;
        }
    }
}
