package com.bnpp.regliss.importer.crb;

// [MISSING FROM SCREENSHOTS: lines 3-13 of the original file were not visible in the photos]

@Slf4j
@Service
@ReglissBatchProfile
public class CRBRecordResolver {

    private CRBRecordResolver() {
    }

    protected static final List<String> headerCells = Stream.of(
            "Securities_id","CRBM ID", "Primary Business Name", "Tier",
            "Security Type", "Security Status", "SEDOL", "CUSIP", "ISIN",
            "Primary Symbol", "Primary Exchange",
            "Second Symbol", "Second Exchange",
            "Third Symbol", "Third Exchange",
            "Fourth Symbol", "Fourth Exchange",
            "Licensed Countries", "License Classifications",
            "License Types (Uniform)", "License Statuses (Uniform)",
            "Sectors", "Dividend", "Pure Play", "% Direct Cannabis Revenue")
            .collect(Collectors.toList());

    public static final Integer SECURITIES_ID_INDEX = 0;
    public static final Integer CRBM_ID_INDEX = 1;
    public static final Integer PRIMARY_BUSINESS_NAME_INDEX = 2;
    public static final Integer TIER_INDEX = 3;
    public static final Integer SECURITY_TYPE_INDEX = 4;
    public static final Integer SECURITY_STATUS_INDEX = 5;
    public static final Integer SEDOL_INDEX = 6;
    public static final Integer CUSIP_INDEX = 7;
    public static final Integer ISIN_INDEX = 8;
    public static final Integer PRIMARY_SYMBOL_INDEX = 9;
    public static final Integer PRIMARY_EXCHANGE_INDEX = 10;
    public static final Integer SECOND_SYMBOL_INDEX = 11;
    public static final Integer SECOND_EXCHANGE_INDEX = 12;
    public static final Integer THIRD_SYMBOL_INDEX = 13;
    public static final Integer THIRD_EXCHANGE_INDEX = 14;
    public static final Integer FOURTH_SYMBOL_INDEX = 15;
    public static final Integer FOURTH_EXCHANGE_INDEX = 16;
    public static final Integer LICENSED_COUNTRIES_INDEX = 17;
    public static final Integer LICENSED_CLASSIFICATION_INDEX = 18;
    public static final Integer LICENSED_TYPES_INDEX = 19;
    public static final Integer LICENSED_STATUSES_INDEX = 20;
    public static final Integer SECTORS_INDEX = 21;
    public static final Integer DIVIDEND_INDEX = 22;
    public static final Integer PURE_PLAY_INDEX = 23;
    public static final Integer PERCENT_DIRECT_CANNABIS_REVENUE_INDEX = 24;

    private static String getProperValue(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\"", "");
    }

    private static CRBRecordReferential createCRBReferencialFromParsedLine(ParsedLine parsedLine) {
        CRBRecordReferential crbRecordReferential = new CRBRecordReferential();
        List<String> row = parsedLine.cells;
        crbRecordReferential.setSecuritiesId(getProperValue(row.get(SECURITIES_ID_INDEX)));
        crbRecordReferential.setPrimaryBusinessName(getProperValue(row.get(PRIMARY_BUSINESS_NAME_INDEX)));
        crbRecordReferential.setTier(getProperValue(row.get(TIER_INDEX)));
        crbRecordReferential.setSecurityType(getProperValue(row.get(SECURITY_TYPE_INDEX)));
        crbRecordReferential.setSecurityStatus(getProperValue(row.get(SECURITY_STATUS_INDEX)));
        crbRecordReferential.setCusip(getProperValue(row.get(CUSIP_INDEX)));
        crbRecordReferential.setIsin(getProperValue(row.get(ISIN_INDEX)));
        crbRecordReferential.setSedol(getProperValue(row.get(SEDOL_INDEX)));
        crbRecordReferential.setPrimarySymbol(getProperValue(row.get(PRIMARY_SYMBOL_INDEX)));
        crbRecordReferential.setPrimaryExchange(getProperValue(row.get(PRIMARY_EXCHANGE_INDEX)));
        crbRecordReferential.setSecondSymbol(getProperValue(row.get(SECOND_SYMBOL_INDEX)));
        crbRecordReferential.setSecondExchange(getProperValue(row.get(SECOND_EXCHANGE_INDEX)));
        crbRecordReferential.setThirdSymbol(getProperValue(row.get(THIRD_SYMBOL_INDEX)));
        crbRecordReferential.setThirdExchange(getProperValue(row.get(THIRD_EXCHANGE_INDEX )));
        crbRecordReferential.setFourthSymbol(getProperValue(row.get(FOURTH_SYMBOL_INDEX)));
        crbRecordReferential.setFourthExchange(getProperValue(row.get(FOURTH_EXCHANGE_INDEX)));
        crbRecordReferential.setLicensedCountries(getProperValue(row.get(LICENSED_COUNTRIES_INDEX)));
        crbRecordReferential.setLicenseClassifications(getProperValue(row.get(LICENSED_CLASSIFICATION_INDEX)));
        crbRecordReferential.setLicenseTypes(getProperValue(row.get(LICENSED_TYPES_INDEX)));
        crbRecordReferential.setLicenseStatuses(getProperValue(row.get(LICENSED_STATUSES_INDEX)));
        crbRecordReferential.setSectors(getProperValue(row.get(SECTORS_INDEX)));
        crbRecordReferential.setDividend(getProperValue(row.get(DIVIDEND_INDEX)));
        crbRecordReferential.setPurePlay(getProperValue(row.get(PURE_PLAY_INDEX)));
        crbRecordReferential.setPercentDirectCannabisRevenue(getProperValue(row.get(PERCENT_DIRECT_CANNABIS_REVENUE_INDEX)));
        return crbRecordReferential;
    }

    private static Record createRecordFromReferential(CRBRecordReferential recordRef) {
        EntityRecord entityRecord = new EntityRecord();
        String notes = calculateNotes(recordRef);
        if (recordRef.getCodeCusip() != null) {
            entityRecord.setCusipCodes(recordRef.getCodeCusip());
        }
        if (recordRef.getCodeIsin() != null) {
            entityRecord.setIsinCodes(recordRef.getCodeIsin());
        }
        entityRecord.setDowJonesNotes(notes);
        entityRecord.setExternalReference(recordRef.getSecuritiesId());
        entityRecord.setName(recordRef.getPrimaryBusinessName());
        entityRecord.setIdentityDocuments(recordRef.getIdentityDocument());
        entityRecord.setPrograms(recordRef.getSanctionProgram());
        return entityRecord;
    }

    private static String calculateNotes(CRBRecordReferential obj) {
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.hasText(obj.getSecurityType())) {
            stringBuilder.append(obj.getSecurityType() + ", ");
        }
        if (StringUtils.hasText(obj.getSecurityStatus())) {
            stringBuilder.append(obj.getSecurityStatus() + ",");
        }
        if (StringUtils.hasText(obj.getSectors())) {
            stringBuilder.append(" Sector: "+ obj.getSectors() + ",");
        }
        if (StringUtils.hasText(obj.getPurePlay())) {
            stringBuilder.append(" Pure Play: "+ obj.getPurePlay() + ",");
        }
        if (StringUtils.hasText(obj.getDividend())) {
            stringBuilder.append(" Dividend: "+ obj.getDividend() + ",");
        }
        if (StringUtils.hasText(obj.getSedol())) {
            stringBuilder.append(" SEDOL: "+ obj.getSedol() + ",");
        }
        if (StringUtils.hasText(obj.getPrimaryExchange())) {
            stringBuilder.append(" Primary Exchange: "+ obj.getPrimaryExchange() + " " + obj.getPrimarySymbol() + ",");
        }
        String licenseNote = "";
        if (StringUtils.hasText(obj.getLicensedCountries())) {
            licenseNote += obj.getLicensedCountries() + ", ";
        }
        if (StringUtils.hasText(obj.getLicenseClassifications())) {
            licenseNote += obj.getLicenseClassifications() + ", ";
        }
        if (StringUtils.hasText(obj.getLicenseTypes())) {
            licenseNote += obj.getLicenseTypes() + ", ";
        }
        if (StringUtils.hasText(obj.getLicenseStatuses())) {
            licenseNote += obj.getLicenseStatuses();
        }
        if (!licenseNote.equals("")) {
            stringBuilder.append(" Licences: " + licenseNote);
        }
        return stringBuilder.toString();
    }

    public static RecordTry<Record> createRecord(ParsedLine parsedLine) {
        Record rec = createRecordFromReferential(createCRBReferencialFromParsedLine(parsedLine));
        return new RecordTry<>(rec);
    }

}

