package com.bnpp.regliss.annotation;

// TODO: the project imports were not visible in the screenshots.
// Re-add them in the IDE (Alt+Enter / Optimize Imports) for: AuditedFieldCode usages (ChildType, SemiAutoImportFieldCategory are defined inline below)

public enum AuditedFieldCode {
    LIST_SHORT_NAME(null, null),
    LIST_NAME(null, null),
    LIST_DESCRIPTION(null, null),
    LIST_TYPE(null, null),
    LIST_CONFIDENTIALITY(null, null),
    LIST_EMAIL(null, null),
    LIST_REFERENCE(null, null),
    LIST_COMMENT_EN(null, null),
    LIST_COMMENT_FR(null, null),
    LIST_SCOPE(null, null),
    LIST_CATEGORY(null, null),
    LIST_TERRITORY(null, null),
    LIST_COUNTRY(null, null),
    LIST_OWNER(null, null),
    LIST_ISSUER(null, null),
    LIST_REGULATOR_IP(null, null),
    LIST_MAPPING_FILE_NAME(null, null),
    LIST_IS_GLOBAL_PLUS_OFAC(null, null),
    ALLOW_INCREMENTAL_IMPORT(null, null),
    GENERIC_FORMAT(null, null),
    REGLISS_FORMAT(null, null),
    SAFEWATCH_FORMAT(null, null),
    FIRCOSOFT_FORMAT(null, null),
    DJ_CORE_XML_FORMAT(null, null),
    VIGILANCE_FORMAT(null, null),
    LIST_GEOGRAPHIC_DATA(null, null),

    LIST_IMPORT_FILE_TYPE(null, null),
    LIST_ZIP_RECEPTION(null, null),
    LIST_CHECKSUM_RECEPTION(null, null),
    LIST_FILE_NAME(null, null),
    LIST_FILE_DIRECTORY(null, null),
    LIST_FLUX_NAME(null, null),
    LIST_LOG_RECEPTION(null, null),
    LIST_ALLOW_AUTO_UPLOAD(null, null),
    LIST_ALLOW_MANUAL_UPLOAD(null, null),
    LIST_CONFIRMATION_REQUIRED(null, null),
    LIST_REGLEMENTATION_FILE(null, null),

    VERSION_LEGAL_BASE(null, null),
    VERSION_START_DATE(null, null),
    VERSION_COMMENT_EN(null, null),
    VERSION_COMMENT_FR(null, null),
    VERSION_REGLEMENTATION(null, null),
    VERSION_RETURN_COMMENT(null, null),

    VERSION_REGLEMENTATION_REFERENCE(null, null),
    VERSION_REGLEMENTATION_TYPE(null, null),
    VERSION_REGLEMENTATION_TITLE(null, null),
    VERSION_REGLEMENTATION_RECEPTION_DATE(null, null),
    VERSION_REGLEMENTATION_COMMENT(null, null),
    VERSION_REGLEMENTATION_PROGRAM(null, null),
    VERSION_REGLEMENTATION_SANCTION_TYPE(null, null),
    VERSION_REGLEMENTATION_URL(null, null),
    VERSION_REGLEMENTATION_OFFICIAL_DATE(null, null),
    VERSION_REGLEMENTATION_FILE(null, null),

    REGLEMENTATION_REFERENCE(null, null),
    REGLEMENTATION_TYPE(null, null),
    REGLEMENTATION_TITLE(null, null),
    REGLEMENTATION_RECEPTION_DATE(null, null),
    REGLEMENTATION_COMMENT(null, null),
    REGLEMENTATION_PROGRAM(null, null),
    REGLEMENTATION_SANCTION_TYPE(null, null),
    REGLEMENTATION_URL(null, null),
    REGLEMENTATION_OFFICIAL_DATE(null, null),
    REGLEMENTATION_FILE(null, null),

    /************************ RECORD ************************/

    RECORD_ID(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_TYPE(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_REGLISS_REFERENCE(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_EXTERNAL_REFERENCE(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_NAME(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_DESCRIPTION(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_DOW_JONES_NOTES(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_KEYWORDS(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_INSTRUCTION(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_COMMENTS(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_ADDITIONAL_INFO(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_START_DATE(SemiAutoImportFieldCategory.GENERAL_INFORMATION,  null),
    RECORD_VERIFICATION_COMMENT(null, null),
    RECORD_VERIFICATION_STATUS(null, null),
    RECORD_VERIFICATION_COMMENT_CREATED_UPDATED(null, null),
    RECORD_VERIFICATION_STATUS_CREATED_UPDATED(null, null),
    RECORD_VERIFICATION_COMMENT_DELETED(null, null),
    RECORD_VERIFICATION_STATUS_DELETED(null, null),
    RECORD_DJ_IMPORT_ACTION(null, null),
    INACTIVITY_DATE_AS_PEP(null, null),
    INACTIVITY_DATE_AS_RCA(null, null),

    RECORD_CATEGORY(SemiAutoImportFieldCategory.RECORD_CATEGORY,  null),
    RECORD_SANCTION_TYPE(SemiAutoImportFieldCategory.SANCTION_TYPE,  null),
    RECORD_DESCRIPTION3(null, null),
    RECORD_OCCUPATION_CATEGORY(null, null),

    PERSON_GENDER(SemiAutoImportFieldCategory.GENDER,  null),
    PERSON_TITLE(SemiAutoImportFieldCategory.PERSON,  null),
    PERSON_FIRST_NAME(SemiAutoImportFieldCategory.PERSON,  null),
    PERSON_MIDDLE_NAME(SemiAutoImportFieldCategory.PERSON,  null),
    PERSON_LAST_NAME(SemiAutoImportFieldCategory.PERSON,  null),
    PERSON_MAIDEN_NAME(SemiAutoImportFieldCategory.PERSON,  null),
    PERSON_SUFFIX(SemiAutoImportFieldCategory.PERSON,  null),

    AIRPORT_SEARCH_CODE(SemiAutoImportFieldCategory.AIRPORT,  null),
    CITY_SEARCH_CODE(SemiAutoImportFieldCategory.CITY,  null),
    COUNTRY_SEARCH_CODE(SemiAutoImportFieldCategory.COUNTRY,  null),
    COUNTRY_ISO_NUM(SemiAutoImportFieldCategory.COUNTRY,  null),
    COUNTRY_ISO_ALPHA(SemiAutoImportFieldCategory.COUNTRY,  null),
    SEAPORT_SEARCH_CODE(SemiAutoImportFieldCategory.SEAPORT,  null),

    OTHER_SEARCH_CODE(SemiAutoImportFieldCategory.OTHER,  null),
    OTHER_CATEGORY(SemiAutoImportFieldCategory.OTHER,  null),

    IBAN_VALUE(SemiAutoImportFieldCategory.IBAN, ChildType.IBAN),
    IBAN_GIVEN_ID(null, ChildType.IBAN),
    BIC_VALUE(SemiAutoImportFieldCategory.BIC, ChildType.BIC),
    BIC_GIVEN_ID(null, ChildType.BIC),
    ISIN_VALUE(SemiAutoImportFieldCategory.ISIN, ChildType.ISIN),
    CUSIP_VALUE(SemiAutoImportFieldCategory.CUSIP, ChildType.CUSIP),

    VESSEL_IMO(SemiAutoImportFieldCategory.VESSEL,  null),
    VESSEL_FLAG(SemiAutoImportFieldCategory.VESSEL,  null),
    VESSEL_OWNER(SemiAutoImportFieldCategory.VESSEL,  null),
    VESSEL_TYPE(SemiAutoImportFieldCategory.VESSEL,  null),
    VESSEL_TONNAGE(SemiAutoImportFieldCategory.VESSEL,  null),
    VESSEL_GROSS_TONNAGE(SemiAutoImportFieldCategory.VESSEL,  null),
    VESSEL_CALL_SIGN(SemiAutoImportFieldCategory.VESSEL,  null),

    RECORD_ADDRESS_ADDRESS(SemiAutoImportFieldCategory.ADDRESS, ChildType.ADDRESS),
    RECORD_ADDRESS_ZIP_CODE(SemiAutoImportFieldCategory.ADDRESS, ChildType.ADDRESS),
    RECORD_ADDRESS_CITY(SemiAutoImportFieldCategory.ADDRESS, ChildType.ADDRESS),
    RECORD_ADDRESS_STATE(SemiAutoImportFieldCategory.ADDRESS, ChildType.ADDRESS),
    RECORD_ADDRESS_COUNTRY(SemiAutoImportFieldCategory.ADDRESS, ChildType.ADDRESS),
    RECORD_ADDRESS_ADDITIONAL_ADDRESS(SemiAutoImportFieldCategory.ADDRESS, ChildType.ADDRESS),
    RECORD_ADDRESS_REMARKS(SemiAutoImportFieldCategory.ADDRESS, ChildType.ADDRESS),
    RECORD_ADDRESS_REGLISS_REFERENCE(null, ChildType.ADDRESS),
    RECORD_ADDRESS_GIVEN_ID(null, ChildType.ADDRESS),

    RECORD_ALIAS_EXTERNAL_REFERENCE(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_TITLE(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_FIRST_NAME(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_MIDDLE_NAME(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_LAST_NAME(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_SUFFIX(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_FULL_NAME(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_TYPE(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_QUALITY(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_LANGUAGE(SemiAutoImportFieldCategory.ALIAS, ChildType.ALIAS),
    RECORD_ALIAS_REGLISS_REFFERENCE(null, ChildType.ALIAS),

    RECORD_SANCTION_REFERENCE_CODE(SemiAutoImportFieldCategory.SANCTION_REFERENCE, ChildType.SANCTION_REFERENCE),
    RECORD_SANCTION_REFERENCE_LIST_NAME(SemiAutoImportFieldCategory.SANCTION_REFERENCE, ChildType.SANCTION_REFERENCE),
    RECORD_SANCTION_REFERENCE_SINCE_DATE(SemiAutoImportFieldCategory.SANCTION_REFERENCE, ChildType.SANCTION_REFERENCE),
    RECORD_SANCTION_REFERENCE_END_DATE(SemiAutoImportFieldCategory.SANCTION_REFERENCE, ChildType.SANCTION_REFERENCE),
    RECORD_SANCTION_REFERENCE_REMARKS(SemiAutoImportFieldCategory.SANCTION_REFERENCE, ChildType.SANCTION_REFERENCE),
    RECORD_SANCTION_REFERENCE_GIVEN_ID(null, ChildType.SANCTION_REFERENCE),

    RECORD_SOURCE_NAME(SemiAutoImportFieldCategory.SOURCE, ChildType.SOURCE),

    // TODO: [GAP] line 205 not captured in screenshots — do NOT fabricate missing logic.

    RECORD_RELATION_NAME(SemiAutoImportFieldCategory.RELATION, ChildType.RELATION),
    RECORD_RELATION_CATEGORY(SemiAutoImportFieldCategory.RELATION, ChildType.RELATION),
    RECORD_RELATION_EX(SemiAutoImportFieldCategory.RELATION, ChildType.RELATION),
    RECORD_RELATION_TYPE(SemiAutoImportFieldCategory.RELATION, ChildType.RELATION),
    RECORD_RELATION_RECORD_TYPE(SemiAutoImportFieldCategory.RELATION, ChildType.RELATION),
    RECORD_RELATION_GIVEN_ID(null, ChildType.RELATION),

    RECORD_ASSOCIATION_EXT_REF(null, ChildType.ASSOCIATION),
    RECORD_ASSOCIATION_TYPE(null, ChildType.ASSOCIATION),
    RECORD_ASSOCIATION_EX(null, ChildType.ASSOCIATION),

    RECORD_PROGRAM_LABEL(SemiAutoImportFieldCategory.PROGRAM, ChildType.PROGRAM),
    RECORD_PROGRAM_DESCRIPTION(SemiAutoImportFieldCategory.PROGRAM, ChildType.PROGRAM),
    RECORD_PROGRAM_SANCTION_TYPE(SemiAutoImportFieldCategory.PROGRAM, ChildType.PROGRAM),
    RECORD_PROGRAM_REFFERENCE_GROUP(SemiAutoImportFieldCategory.PROGRAM, ChildType.PROGRAM),
    RECORD_PROGRAM_GIVEN_ID(null, ChildType.PROGRAM),

    RECORD_IDENTITY_DOCUMENT_TYPE(SemiAutoImportFieldCategory.IDENTITY, ChildType.IDENTITY),
    RECORD_IDENTITY_DOCUMENT_NUMBER(SemiAutoImportFieldCategory.IDENTITY, ChildType.IDENTITY),
    RECORD_IDENTITY_DOCUMENT_COUNTRY(SemiAutoImportFieldCategory.IDENTITY, ChildType.IDENTITY),
    RECORD_IDENTITY_DOCUMENT_ISSUING_DATE(SemiAutoImportFieldCategory.IDENTITY, ChildType.IDENTITY),
    RECORD_IDENTITY_DOCUMENT_EXPIRATION_DATE(SemiAutoImportFieldCategory.IDENTITY, ChildType.IDENTITY),
    RECORD_IDENTITY_DOCUMENT_ADDITIONAL_INFO(SemiAutoImportFieldCategory.IDENTITY, ChildType.IDENTITY),
    RECORD_IDENTITY_DOCUMENT_REFFERENCE_GROUP(SemiAutoImportFieldCategory.IDENTITY, ChildType.IDENTITY),
    RECORD_IDENTITY_DOCUMENT_GIVEN_ID(null, ChildType.IDENTITY),

    RECORD_DATE_DETAILS_DATE_VALUE(SemiAutoImportFieldCategory.DATE_DETAILS, ChildType.DATE_DETAILS),
    RECORD_DATE_DETAILS_DATE_TYPE(SemiAutoImportFieldCategory.DATE_DETAILS, ChildType.DATE_DETAILS),
    RECORD_DATE_DETAILS_GIVEN_ID(null, ChildType.DATE_DETAILS),

    RECORD_PERSON_BIOGRAPHY_DATE_VALUE(SemiAutoImportFieldCategory.PERSON_BIOGRAPHY, ChildType.PERSON_BIOGRAPHY),
    RECORD_PERSON_BIOGRAPHY_DATE_TYPE(SemiAutoImportFieldCategory.PERSON_BIOGRAPHY, ChildType.PERSON_BIOGRAPHY),
    RECORD_PERSON_BIOGRAPHY_BIRTH_PLACE(SemiAutoImportFieldCategory.PERSON_BIOGRAPHY, ChildType.PERSON_BIOGRAPHY),
    RECORD_PERSON_BIOGRAPHY_BIRTH_COUNTRY(SemiAutoImportFieldCategory.PERSON_BIOGRAPHY, ChildType.PERSON_BIOGRAPHY),
    RECORD_PERSON_BIOGRAPHY_IS_PRIMARY(SemiAutoImportFieldCategory.PERSON_BIOGRAPHY, ChildType.PERSON_BIOGRAPHY),
    RECORD_PERSON_BIOGRAPHY_ADDITIONAL_INFO(SemiAutoImportFieldCategory.PERSON_BIOGRAPHY, ChildType.PERSON_BIOGRAPHY),
    PERSON_IS_DECEASED(SemiAutoImportFieldCategory.PERSON_BIOGRAPHY, ChildType.PERSON_BIOGRAPHY),
    RECORD_PERSON_BIOGRAPHY_GIVEN_ID(null, ChildType.PERSON_BIOGRAPHY),

    RECORD_COUNTRY_DETAILS_TYPE(SemiAutoImportFieldCategory.COUNTRY_DETAILS, ChildType.COUNTRY_DETAILS),
    RECORD_COUNTRY_DETAILS_BIRTH_COUNTRY(SemiAutoImportFieldCategory.COUNTRY_DETAILS, ChildType.COUNTRY_DETAILS),
    RECORD_COUNTRY_DETAILS_IS_PRIMARY(SemiAutoImportFieldCategory.COUNTRY_DETAILS, ChildType.COUNTRY_DETAILS),
    RECORD_COUNTRY_DETAILS_ADDITIONAL_INFO(SemiAutoImportFieldCategory.COUNTRY_DETAILS, ChildType.COUNTRY_DETAILS),
    RECORD_COUNTRY_DETAILS_GIVEN_ID(null, ChildType.COUNTRY_DETAILS),

    PERSON_FUNCTION_NAME(SemiAutoImportFieldCategory.FUNCTION, ChildType.FUNCTION),
    PERSON_FUNCTION_CATEGORY(SemiAutoImportFieldCategory.FUNCTION, ChildType.FUNCTION),
    PERSON_FUNCTION_FROM_DATE(SemiAutoImportFieldCategory.FUNCTION, ChildType.FUNCTION),
    PERSON_FUNCTION_TO_DATE(SemiAutoImportFieldCategory.FUNCTION, ChildType.FUNCTION),
    PERSON_FUNCTION_GIVEN_ID(null, ChildType.FUNCTION),

    PERSON_IMAGE_URL(SemiAutoImportFieldCategory.IMAGE, ChildType.IMAGE),

    /************************ SUBSCRIPTION ************************/
    SUBSCRIBER_NAME(null, null),
    SUBSCRIBER_FLUX_NAME(null, null),
    SUBSCRIBER_DESCRIPTION(null, null),
    SUBSCRIBER_CONTACT(null, null),
    SUBSCRIBER_ENTITY_OR_SECTOR(null, null),
    SUBSCRIBER_COUNTRY(null, null),
    SUBSCRIBER_HAS_DELIVERY_REPORT(null, null),
    SUBSCRIBER_HOUR_OF_DIFFUSION(null, null),
    SUBSCRIBER_PARTNER(null, null),
    SUBSCRIBER_RECEIVER_KEY_ID(null, null),
    SUBSCRIBER_WL_FULL_BROADCAST_FREQUENCY(null, null),
    SUBSCRIBER_WL_DELTA_BROADCAST_FREQUENCY(null, null),
    SUBSCRIBER_WATCHLIST_BROADCAST_FREQUENCY(null, null),
    SUBSCRIBER_ENCRYPTION(null, null),

    /************************ SUBSTITUTION_MATRIX ************************/

    SUBSTITUTION_MATRIX_FILE_FORMAT(null, null),
    SUBSTITUTION_MATRIX_ORIGINAL_VALUE(null, null),
    SUBSTITUTION_MATRIX_SUBSTITUTED_VALUE(null, null),

    /************************ SIX_FILTER_XML ************************/
    FILTER_FILE_TYPE(null, null),
    FILTER_FIELD_NAME(null, null),
    FILTER_OPERATOR(null, null),
    FILTER_VALUE(null, null),

    /************************ DJ_WATCHLIST_XML ************************/
    FILTER_NAME(null, null),
    FILTER_TAG(null, null),
    FILTER_FORMAT(null, null),
    FILTER_COMMENT(null, null),
    FILTER_TYPE(null, null),
    FILTER_EXTENSION(null, null),
    FILTER_ACTIVE(null, null),
    FILTER_DELETE(null, null),
    ADD_EXCLUDE_CMIC(null, null),
    ADD_EXCLUDE_EO14071(null, null),

    SUBFILTER_SINCE_DEATH_PEP_SIP(null, null),
    SUBFILTER_SINCE_DEATH_RCA(null, null),
    SUBFILTER_INACTIVITY_PEP(null, null),
    SUBFILTER_INACTIVITY_RCA(null, null),
    SUBFILTER_DESCRIPTIONS1(null, null),
    SUBFILTER_DESCRIPTIONS2(null, null),
    SUBFILTER_DESCRIPTIONS3(null, null),
    SUBFILTER_PEP_OCCUPATIONS(null, null),
    SUBFILTER_RESIDENCE_COUNTRIES(null, null),
    SUBFILTER_JURISDICTION_COUNTRIES(null, null),
    SUBFILTER_RELATION_TYPES(null, null),
    SUBFILTER_GENDERS(null, null),
    SUBFILTER_NAME(null, null),

    USER_PROFILE(null, null),
    USER_LANGUAGE(null, null),
    USER_PERMISSION(null, null),
    USER_DOWNLOAD_EXPORT_PERMISSION_END_DATE(null, null),
    USER_DOWNLOAD_IMPORT_PERMISSION_END_DATE(null, null),
    USER_UID(null, null),
    USER_FIRST_NAME(null, null),
    USER_LAST_NAME(null, null),
    USER_EMAIL(null, null),
    USER_ENTITY(null, null),
    USER_POLE(null, null),
    USER_COUNTRY_NAME(null, null),
    USER_BUSINESS_LINE(null, null),

    NOT_MAPPED(SemiAutoImportFieldCategory.NOT_MAPPED,  null),
    RECORD_OTHER_CHANGES(null, null),

    ;

    public SemiAutoImportFieldCategory getCategory() { return category; }

    private final SemiAutoImportFieldCategory category;
    private final ChildType childType;

    AuditedFieldCode(SemiAutoImportFieldCategory category, ChildType childType) {
        this.category = category;
        this.childType = childType;
    }
    public ChildType getChildType() { return childType; }
    public boolean isMapped() { return category != null; }

    // WARNING: The order that these enums appear is reflected in the UI (field combobox - Manual Import)
    public enum SemiAutoImportFieldCategory {
        GENERAL_INFORMATION,

        SANCTION_TYPE,
        RECORD_CATEGORY,

        PERSON,
        ENTITY,
        VESSEL,
        AIRPORT,
        CITY,
        COUNTRY,
        OTHER,
        SEAPORT,

        // DO NOT REORDER this ENUM !!

        FUNCTION,
        ALIAS,
        PROGRAM,
        ADDRESS,
        IDENTITY,
        DATE_DETAILS,
        PERSON_BIOGRAPHY,
        COUNTRY_DETAILS,
        SANCTION_REFERENCE,
        SOURCE,

        RELATION,
        ASSOCIATION,
        FUNCTION,

        IMAGE,
        IBAN,
        BIC,
        ISIN,
        CUSIP,
        OTHERCODE,

        NOT_MAPPED
    }
}
