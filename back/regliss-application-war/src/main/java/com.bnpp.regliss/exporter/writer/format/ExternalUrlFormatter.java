package com.bnpp.regliss.exporter.writer.format;

// [MISSING FROM SCREENSHOTS: lines 3-12 of the original file were not visible in the photos]

@Component
public class ExternalUrlFormatter {
    @Value("${external.element.detail.html.url}")
    private String externalElementDetailHtmlUrl;
    @Autowired
    private MessageSource messageSource;

    private static final String CONFIDENTIAL_URL_MESSAGE_KEY = "external.url.confidential.message";

    public String getRecordUrls(RegularExportElementData recordAndVersion) {
        String fr = getRecordFrenchUrlWithLabel(recordAndVersion);
        String en = getRecordEnglishUrlWithLabel(recordAndVersion);
        return fr + " " + en;
    }

    public String getRecordFrenchUrlWithLabel(RegularExportElementData recordAndVersion) {
        return getRecordFrenchUrl(recordAndVersion.getRecord(),
                recordAndVersion.getVersion().getList().getConfidentiality());
    }
    public String getRecordEnglishUrlWithLabel(RegularExportElementData recordAndVersion) {
        return FircosoftExportFormatWriter.getFieldLimit(getRecordEnglishUrl(recordAndVersion.getRecord(),
                recordAndVersion.getVersion().getList().getConfidentiality()), 1024);
    }

    public String getRecordEnglishUrl(Record r, Confidentiality listConfidentiality) {
        return Confidentiality.CONFIDENTIAL != listConfidentiality ?
                getRecordNonConfidentialEnglishUrl(r.getExternalUuid()) :
                getRecordConfidentialEnglishUrl(r.getReglissReference());
    }

    public String getRecordFrenchUrl(Record r, Confidentiality listConfidentiality) {
        return Confidentiality.CONFIDENTIAL != listConfidentiality ?
                getRecordNonConfidentialFrenchUrl(r.getExternalUuid()) :
                getRecordConfidentialFrenchUrl(r.getReglissReference());
    }

    public String getRecordNonConfidentialUrl(String externalUUID, Locale locale) {
        if (locale == Locale.FRENCH) {
            return getRecordNonConfidentialFrenchUrl(externalUUID);
        } else {
            return getRecordNonConfidentialEnglishUrl(externalUUID);
        }
    }

    public String getConfidentialUrl(String externalUUID, Locale locale) {
        return String.format(messageSource.getMessage(CONFIDENTIAL_URL_MESSAGE_KEY, null, locale), externalUUID);
    }

    public String getRecordUrl(Confidentiality listConfidentiality, String externalUUID, Locale locale) {
        return Confidentiality.CONFIDENTIAL != listConfidentiality ?
                getRecordNonConfidentialUrl(externalUUID, locale) :
                getConfidentialUrl(externalUUID, locale);
    }

    String getRecordNonConfidentialEnglishUrl(String externalUUID) {
        String anchor = createExternalRecordUrlPrefix(externalUUID);
        return concatUrlParts(anchor, "?lang=EN");
    }

    String getRecordNonConfidentialFrenchUrl(String externalUUID) {
        String anchor = createExternalRecordUrlPrefix(externalUUID);
        return concatUrlParts(anchor, "?lang=FR");
    }

    private String createExternalRecordUrlPrefix(String externalUUID) {
        return "/app/index.html#!/records/externe/" + externalUUID;
    }

    private String concatUrlParts(String anchor, String langQueryParam) {
        return externalElementDetailHtmlUrl + anchor + langQueryParam;
    }

    private String getRecordConfidentialEnglishUrl(String externalUUID) {

        return getConfidentialUrl(externalUUID, Locale.ENGLISH);
    }

    private String getRecordConfidentialFrenchUrl(String externalUUID) {

        return getConfidentialUrl(externalUUID, Locale.FRENCH);
    }

}
