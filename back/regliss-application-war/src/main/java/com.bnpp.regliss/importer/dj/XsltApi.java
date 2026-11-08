package com.bnpp.regliss.importer.dj;

// [MISSING FROM SCREENSHOTS: lines 3-27 of the original file were not visible in the photos]

@Slf4j
@Component
@ReglissBatchProfile
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS) // in order to report unique INSERTs in the console (statefull per file
public class XsltApi {

    public static final String DATE_PATTERN = "dd/MM/yyyy";
    // !!!! STATE specific to a particular import execution !!!!
    private ImportFileType format;
    private Set<String> warningMessages = new TreeSet<>();

    private DbReferentials dbReferentials;
    private DJXmlReferentials xmlReferentials;

    public void setXmlReferentials(DJXmlReferentials xmlReferentials) { this.xmlReferentials = xmlReferentials; }

    public void setDbReferentials(DbReferentials dbReferentials) { this.dbReferentials = dbReferentials; }

    public void initialize(ImportFileType format) { this.format = format; }

    public static XsltApi getInstanceFromSpring(String djFileType) {
        XsltApi bean = StaticContextAccessor.getBean(XsltApi.class);
        bean.initialize(ImportFileType.valueOf(djFileType));
        return bean;
    }

    public static String removeDuplicates(String commaSeparatedValues) {
        String[] values = commaSeparatedValues.split(",");
        return String.join(", ", (new HashSet<>(asList(values))));
    }

    public void printAllReferentialsWarningMessages() {
        if (!warningMessages.isEmpty()) {
            String scriptsCollected = String.join("\n", warningMessages);
            log.warn("Following referentials warning messages collected: \n{}", scriptsCollected);
        }
    }

    public String resolveRefInDbByCode(String refTypeStr, String refCode) {
        if (!StringUtils.isEmpty(refTypeStr) && !StringUtils.isBlank(refCode)) {
            Optional<String> labelInFile = getLabelInFileByCode(refCode, DJReferentialType.valueOf(refTypeStr));
            if (labelInFile.isPresent()) {
                return resolveRefInDbByLabel(refTypeStr, labelInFile.get());
            } else {
                warningMessages.add(format("No label in file for type: %s and code: %s for format: %s", refTypeStr, refCode, format));
            }
        }
        return "";
    }

    public String resolveRefInDbByCodeAndParent(String refTypeStr, String refCode, String parentCode, String parentId) {
        if (!StringUtils.isEmpty(refTypeStr) && !StringUtils.isBlank(refCode)) {
            DJReferentialType refType = DJReferentialType.valueOf(refTypeStr);
            Optional<String> labelInFile = getLabelInFileByCodeAndParentCode(refCode, parentCode, refType);
            if (labelInFile.isPresent()) {
                return resolveRefInDbByLabel(refTypeStr, labelInFile.get() + ":" + parentId);
            } else {
                warningMessages.add(format("No label in file for type: %s and code: %s and parentCode: %s for format: %s", refTypeStr,
                        refCode, parentCode, format));
            }
        }
        return "";
    }

    public String resolveRefInDbByLabelAllowEmpty(String refTypeStr, String refLabel) {
        if(refLabel==null || refLabel.isEmpty()) return null;
        return resolveRefInDbByLabel(refTypeStr,refLabel);
    }
    public String resolveRefInDbByLabel(String refTypeStr, String refLabel) {
        Long idInDb;
        DJReferentialType refType = DJReferentialType.valueOf(refTypeStr);
        Map<String, Long> labelToId = dbReferentials.get(refType);
        if(refType.equals(DJReferentialType.DATE_TYPE)){
            labelToId=this.tranformDateTypeMap(labelToId);
        }
        if (labelToId.containsKey(refLabel.trim())) {
            idInDb = labelToId.get(refLabel.trim());

        } else {
            warningMessages.add(format("No referential value in database for type: %s, with label: %s", refTypeStr, refLabel));
            idInDb = labelToId.get(ReferentialBusinessConcept.DEFAULT_VALUE.name());
        }
        if (idInDb != null) {
            return String.valueOf(idInDb);
        }
        return "";
    }

    public String resolveRefInFileByCode(String refCode, String refTypeStr) {
        return getLabelInFileByCode(refCode, DJReferentialType.valueOf(refTypeStr))
                .orElse("");
    }

    public String resolveRefsInFileByCodes(String refCodes, String refTypeStr, String separator) {
        return stream(refCodes.split(separator))
                .map(c -> resolveRefInFileByCode(c, refTypeStr))
                .collect(joining(separator)).trim();
    }

    public String resolveSanctionReferenceGroupCodeForCore(String sanctionReferenceCode) {
        return xmlReferentials.get(DJReferentialType.SANCTION_REFERENCE).stream()
                .filter(ref -> Objects.equals(ref.getCode(), sanctionReferenceCode))
                .map(ref -> ((DJXmlReferentialWithReferenceGroupCode) ref).getReferenceGroupCode())
                .findAny()
                .orElse("");
    }

    public String resolveAliasType(String label) {
        if (!StringUtils.isBlank(label)) {
            Optional<ReferentialBusinessConcept> aliasTypeBusinessConcept = getAliasTypesByFormat().entrySet().stream()
                    .filter(e -> label.startsWith(e.getKey().getValue()))
                    .map(Entry::getValue)
                    .findFirst();
            return aliasTypeBusinessConcept
                    .map(a -> resolveRefInDbByBusinessConcept(DJReferentialType.ALIAS_TYPE, a))
                    .orElse(EMPTY);
        }
        return EMPTY;
    }

    public String resolveAliasQualityWatchlist(String label) {
        if (!StringUtils.isBlank(label)) {
            Optional<ReferentialBusinessConcept> aliasQualityBusinessConcept = getAliasQualities().entrySet().stream()
                    .filter(e -> label.startsWith(e.getKey().getValue()))
                    .map(Entry::getValue)
                    .findFirst();
            return aliasQualityBusinessConcept
                    .map(a -> resolveRefInDbByBusinessConcept(DJReferentialType.ALIAS_QUALITY, a))
                    .orElse(EMPTY);
        }
        return EMPTY;
    }

    public String resolveRefInDbByBusinessConcept(String refTypeStr, String businessConcept) {
        return resolveRefInDbByBusinessConcept(DJReferentialType.valueOf(refTypeStr), ReferentialBusinessConcept.valueOf(businessConcept));
    }

    private String resolveRefInDbByBusinessConcept(DJReferentialType refType, ReferentialBusinessConcept businessConcept) {
        Long idInDb;
        Map<String, Long> labelToId = dbReferentials.get(refType);
        if(labelToId.containsKey(businessConcept.name())) {
            idInDb = labelToId.get(businessConcept.name());
        } else {
            warningMessages.add(format("No referential value in database for type: %s, with business concept: %s", refType, businessConcept));
            idInDb = labelToId.get(ReferentialBusinessConcept.DEFAULT_VALUE.name());
        }
        if(idInDb != null) {
            return String.valueOf(idInDb);
        }
        return "";
    }

    private Optional<String> getLabelInFileByCodeAndParentCode(String refCode, String parentCode,
                                                               DJReferentialType refType) {
        return xmlReferentials.get(refType).stream()
                .filter(DJXmlReferentialWithParentCode.byCodeAndParent(refCode.trim(), parentCode.trim()))
                .map(DJXmlReferential::getValue)
                .findFirst();
    }

    private Optional<String> getLabelInFileByCode(String refCode, DJReferentialType refType) {
        final String refCodeTrimmed = refCode.trim();
        return xmlReferentials.get(refType).stream()
                .filter(ref -> Objects.equals(ref.getCode(), refCodeTrimmed))
                .map(DJXmlReferential::getValue)
// [MISSING FROM SCREENSHOTS: lines 200-220 of the original file were not visible in the photos]
    /*Name processing logic for AKAs line 16,17,19 of file "Mapping Sanctions GUI V2.6.xslx -> sheet Name Processing Logic"*/
    public static boolean shouldNamePartsOtherThanLastNameBeCompleted(String title, String firstName, String middleName, String lastName,
                                                                      String suffix) {
        if (isBlank(title) && isNotBlank(firstName) && isBlank(middleName) && isBlank(lastName) && isNotBlank(suffix)) {
            return false;
        }
        if (isBlank(title) && isNotBlank(firstName) && isBlank(middleName) && isBlank(lastName) && isBlank(suffix)) {
            return false;
        }
        if (isBlank(title) && isBlank(firstName) && isNotBlank(middleName) && isBlank(lastName) && isBlank(suffix)) {
            return false;
        }
        if (isBlank(title) && isBlank(firstName) && isNotBlank(middleName) && isBlank(lastName) && isNotBlank(suffix)) {
            return false;
        }

        if (isBlank(title) && isNotBlank(firstName) && isNotBlank(middleName) && isBlank(lastName) && isBlank(suffix)) { //NOSONAR we dont simpl
            return false;
        }
        //ALM 578
        return !isNotBlank(title) || !isNotBlank(firstName) || !isBlank(middleName) || !isBlank(lastName) || !isBlank(suffix);
    }

    //ALM 578
    public static boolean shouldTitleNameBeCompleted(String title, String firstName, String middleName, String lastName,
                                                     String suffix) {
        return isNotBlank(title) && isNotBlank(firstName) && isBlank(middleName) && isBlank(lastName) && isBlank(suffix);
    }


    public static String toXsdDateWithDefault(String dateStr, String format, String defaultValue) {
        if ("--".equals(dateStr)) {
            return defaultValue;
        }
        try {
            Date date = new SimpleDateFormat(format, Locale.ENGLISH).parse(dateStr);
            return new SimpleDateFormat("yyyy-MM-dd").format(date);
        } catch (ParseException ex) {
            log.trace("Error while parsing the date: {}", dateStr);
        }
        return defaultValue;
    }

    public static String toXsdDate(String dateStr, String format) {
        if ("--".equals(dateStr)) {
            return null;
        }
        try {
            Date date = new SimpleDateFormat(format, Locale.ENGLISH).parse(dateStr);
            return new SimpleDateFormat(DATE_PATTERN).format(date);
        } catch (ParseException ex) {
            log.trace("Error while parsing the date: {}", dateStr);
        }
        return null;
    }

    public String toBoolean(String value) {
        if ("yes".equalsIgnoreCase(value) || "1".equalsIgnoreCase(value)||"true".equalsIgnoreCase(value)) {
            return String.valueOf(true);
        } else if ("no".equalsIgnoreCase(value) || "0".equalsIgnoreCase(value)||"false".equalsIgnoreCase(value)) {
            return String.valueOf(false);
        } else {
            return "";
        }
    }


    private Map<XmlBusinessAttributes, ReferentialBusinessConcept> getAliasTypesByFormat() {
        Map<XmlBusinessAttributes, ReferentialBusinessConcept> aliasTypes = new EnumMap<>(XmlBusinessAttributes.class);
        if (format == ImportFileType.DOW_JONES_CORE) {
            aliasTypes.put(XmlBusinessAttributes.CORE_AKA, ReferentialBusinessConcept.ALIAS_TYPE_AKA);
            aliasTypes.put(XmlBusinessAttributes.ORIGINAL_SCRIPT_NAME, ReferentialBusinessConcept.ALIAS_TYPE_ORIGINAL_SCRIPT_NAME);
            aliasTypes.put(XmlBusinessAttributes.SINGLE_STRING_NAME, ReferentialBusinessConcept.ALIAS_TYPE_SINGLE_SCRIPT_NAME);
            aliasTypes.put(XmlBusinessAttributes.LOW_QUALITY_AKA, ReferentialBusinessConcept.ALIAS_TYPE_AKA);
            aliasTypes.put(XmlBusinessAttributes.RECLASSIFIED_AKA, ReferentialBusinessConcept.ALIAS_TYPE_RECLASSIFIED_AKA);
        } else {
            aliasTypes.put(XmlBusinessAttributes.WATCHLIST_ALSO_KNOWN_AS, ReferentialBusinessConcept.ALIAS_TYPE_AKA);
            aliasTypes.put(XmlBusinessAttributes.WATCHLIST_EXPANDED_LANGUAGE_VARIATION, ReferentialBusinessConcept.DEFAULT_VALUE);
            aliasTypes.put(XmlBusinessAttributes.WATCHLIST_FORMERLY_KNOWN_AS, ReferentialBusinessConcept.ALIAS_TYPE_FKA);
            aliasTypes.put(XmlBusinessAttributes.WATCHLIST_SPELLING_VARIATION, ReferentialBusinessConcept.ALIAS_TYPE_SPELLING_VARIATION);
            aliasTypes.put(XmlBusinessAttributes.ORIGINAL_SCRIPT_NAME, ReferentialBusinessConcept.ALIAS_TYPE_ORIGINAL_SCRIPT_NAME);
            aliasTypes.put(XmlBusinessAttributes.SINGLE_STRING_NAME, ReferentialBusinessConcept.ALIAS_TYPE_SINGLE_SCRIPT_NAME);
            aliasTypes.put(XmlBusinessAttributes.LOW_QUALITY_AKA, ReferentialBusinessConcept.ALIAS_TYPE_LOW_QUALITY_AKA);
            aliasTypes.put(XmlBusinessAttributes.WATCHLIST_MAIDEN_NAME, ReferentialBusinessConcept.ALIAS_TYPE_MAIDEN_NAME);
        }
        return aliasTypes;
    }

    private Map<XmlBusinessAttributes, ReferentialBusinessConcept> getAliasQualities() {
        //TODO add referiantials from v3
        Map<XmlBusinessAttributes, ReferentialBusinessConcept> aliasQualities = new EnumMap<>(XmlBusinessAttributes.class);
        aliasQualities.put(XmlBusinessAttributes.WATCHLIST_ALSO_KNOWN_AS, ReferentialBusinessConcept.ALIAS_QUALITY_STRONG);
        aliasQualities.put(XmlBusinessAttributes.LOW_QUALITY_AKA, ReferentialBusinessConcept.ALIAS_QUALITY_WEAK);
        aliasQualities.put(XmlBusinessAttributes.WATCHLIST_EXPANDED_LANGUAGE_VARIATION, ReferentialBusinessConcept.ALIAS_QUALITY_STRONG);
        aliasQualities.put(XmlBusinessAttributes.WATCHLIST_FORMERLY_KNOWN_AS, ReferentialBusinessConcept.ALIAS_QUALITY_STRONG);
        aliasQualities.put(XmlBusinessAttributes.WATCHLIST_SPELLING_VARIATION, ReferentialBusinessConcept.ALIAS_QUALITY_STRONG);
        aliasQualities.put(XmlBusinessAttributes.SINGLE_STRING_NAME, ReferentialBusinessConcept.ALIAS_QUALITY_WEAK);
        aliasQualities.put(XmlBusinessAttributes.ORIGINAL_SCRIPT_NAME, ReferentialBusinessConcept.ALIAS_QUALITY_WEAK);
        aliasQualities.put(XmlBusinessAttributes.WATCHLIST_MAIDEN_NAME, ReferentialBusinessConcept.ALIAS_QUALITY_STRONG);
        return aliasQualities;
    }

    public String tranformInLowerCase(String value) {
        if (!StringUtils.isEmpty(value)) {
            return value.toLowerCase();
        }
        return "";
    }

    public String resolveAliasQuality(String valueAliasQuality){
        final String aliasQuality = "ALIAS_QUALITY";
        if (StringUtils.isEmpty(valueAliasQuality) || "strong".equalsIgnoreCase(valueAliasQuality.trim())){
            return this.resolveRefInDbByLabel(aliasQuality, "Strong" );
        }
        else if( "weak".equalsIgnoreCase(valueAliasQuality.trim())){
            return this.resolveRefInDbByLabel(aliasQuality, "Weak");
        }
        return  this.resolveRefInDbByLabel(aliasQuality,  valueAliasQuality);
    }

    public String isPersonBiografy(String label) {
        if (!StringUtils.isBlank(label)) {
            Optional<String> dateTypeBusinessConcept = dbReferentials.get(DJReferentialType.DATE_TYPE).keySet().stream()
                    .filter(key -> key.startsWith(label)).findFirst();
            if (dateTypeBusinessConcept.isPresent()) {
                if(dateTypeBusinessConcept.get().contains(ReferentialBusinessConcept.DATE_TYPE_DATE_OF_BIRTH.name())){
                    return "1";
                }

                if(dateTypeBusinessConcept.get().contains(ReferentialBusinessConcept.DATE_TYPE_DATE_OF_DEATH.name())){
                    return "2";
                }

            }

        }
        return "0";
    }

    private Map<String,Long> tranformDateTypeMap(Map<String,Long> dateTypeRef){
        Map<String,Long> result=new HashMap<>();
        dateTypeRef.forEach((key,value)->result.put(key.contains("+")?key.substring(0,key.indexOf('+')):key,value));
        return result;
    }

}

