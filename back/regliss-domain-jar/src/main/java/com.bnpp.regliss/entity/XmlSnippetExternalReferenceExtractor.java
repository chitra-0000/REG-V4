package com.bnpp.regliss.entity;

import com.bnpp.regliss.ReglissException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class XmlSnippetExternalReferenceExtractor {
    private static final Pattern extractIdPattern = Pattern.compile("id=\"([^\"]+)\"");
    private static final Pattern extractIdCustomImportPattern = Pattern.compile("externalReference=\"([^\"]+)\"");

    public static String extractIdFromXml(String originalXmlSnippet, ImportFileType format) {
        Matcher matcher = null;
        if (format.isDjFormat()) {
            matcher = extractIdPattern.matcher(originalXmlSnippet);
        } else {
            matcher = extractIdCustomImportPattern.matcher(originalXmlSnippet);
        }

        if (!matcher.find()) {
            throw new ReglissException("There is no externalRefernce attribute in xml snippet: \n" + originalXmlSnippet, ReglissException.ErrorCode.IMPORT_RECORD_MISSING_EXTERNAL_REFERENCE);
        }
        return matcher.group(1);
    }

}
