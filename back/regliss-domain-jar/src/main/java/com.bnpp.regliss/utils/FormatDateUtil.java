package com.bnpp.regliss.utils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FormatDateUtil {

    private FormatDateUtil() {
//
    }

    private static final String UNKNOWN_DATE_FORMAT = "????-??-??";
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ISO_DATE;
    public static final String DATE_TIME_FORMAT_WITH_LINE_AUDIT_TRAIL = "yyyyMMdd-HHmmss";


    public static String handleWrongFormatDate(String dateStr) {
        String formatedDateStr = dateStr;
        if (dateStr == null) {
            return UNKNOWN_DATE_FORMAT;
        }
        try {
            LocalDate.parse(dateStr, DATE_PATTERN);
            return  dateStr;
        } catch (DateTimeParseException e) {
            //
        }

        List<DateTimeFormatter> wrongFormatters = Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MMM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MMM-yy"),
                DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        for (DateTimeFormatter formatter :wrongFormatters) {
            try {
                LocalDate wrongFormatDate = LocalDate.parse(dateStr, formatter);
                formatedDateStr = getDateStringFromLocalDate(wrongFormatDate);
                break;
            } catch (DateTimeParseException e1) {
                //
            }
        }

        if (dateStr.equals(formatedDateStr)) {
            formatedDateStr = yearMonthParse(dateStr);
        }

        if (dateStr.length() == 4 && NumberUtils.isParsable(dateStr)) {
            return dateStr + "-??-??";
        }

        return reverseUnkownDate(formatedDateStr);
    }

    public static String yearMonthParse(String dateStr) {
        dateStr = dateStr.replace("/", "-").replace(".", "-");
        List<DateTimeFormatter> wrongYMFormatters = Arrays.asList(DateTimeFormatter.ofPattern("MMM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MMM"),
                DateTimeFormatter.ofPattern("yyyy-MM"),
                DateTimeFormatter.ofPattern("MM-yyyy"));
        for (DateTimeFormatter formatter :wrongYMFormatters) {
            try {
                YearMonth yearMonth = YearMonth.parse(dateStr, formatter);
                return yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")) + "-??";
            } catch (DateTimeParseException e) {
                //
            }
        }
        return dateStr;
    }

    public static String reverseUnkownDate(String dateStr) {
        if (dateStr != null) {
            Optional<Character> separatorCharOpt = dateStr.chars().mapToObj(x -> (char) x).filter(c -> !c.equals('?')).filter(c -> !Character.isDigit(c)).findFirst();
            if (separatorCharOpt.isPresent()) {
                Character separatorChar = separatorCharOpt.get();
                String[] array = dateStr.split("["+separatorChar+"]");
                if (array.length > 0 && array[0].length() < 4) {
                    ArrayUtils.reverse(array);

                    dateStr = Stream.of(array).map(FormatDateUtil::toDoubleDigitIfSingleDigit).collect(Collectors.joining("-"));
                }
            }
        }

        return dateStr;
    }

    public static Optional<LocalDate> getLocalDateOptFromDateString(String dateStr) {
        if (dateStr == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(dateStr, DATE_PATTERN));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static String getDateStringFromLocalDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        try {
            return date.format(DATE_PATTERN);
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static String toDoubleDigitIfSingleDigit(String e) {
        return (e.length() == 1) ? "0" + e : e;
    }
}
