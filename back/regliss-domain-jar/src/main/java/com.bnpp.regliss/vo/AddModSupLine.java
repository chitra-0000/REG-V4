package com.bnpp.regliss.vo;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.entity.referential.record.RecordType;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bnpp.regliss.vo.AddModSupElementType.*;
import static java.util.stream.Collectors.joining;

@Data
public class AddModSupLine {

    private static final String ALIAS = "Alias";
    private final AddModSupAction change;
    private final String versionOfficialDate;
    private final RecordType recordType;
    private final String recordReglissReference;
    private final String akaReglissReferenceRaw;

    private final AddModSupElementType modElement;
    private final String fullNameRaw;
    private final String newValueRaw;
    private final String oldValueRaw;
    private final String recordId;
    private final long id;

    private final Long childId;
    private final AuditedAction auditedAction;
    private final ChildType childType;

    public AddModSupLine(RecordType recordType, String recordReglissReference, String fullNameRaw, //NOSONAR project specific
            AddModSupElementType modElement, String oldValueRaw, String newValueRaw, AddModSupAction change,
            String akaReglissreferenceRaw, String versionOfficialDate, String recordId, long id,
                         Long childId, AuditedAction auditedAction, ChildType childType) {
        this.recordType = recordType;
        this.recordReglissReference = recordReglissReference;
        this.fullNameRaw = fullNameRaw;
        this.modElement = modElement;
        this.oldValueRaw = oldValueRaw;
        this.newValueRaw = newValueRaw;
        this.change = change;
        this.akaReglissReferenceRaw = akaReglissreferenceRaw;
        this.versionOfficialDate = versionOfficialDate;
        this.recordId = recordId;
        this.id = id;
        this.childId = childId;
        this.auditedAction = auditedAction;
        this.childType = childType;
    }

    public AddModSupLine(Object[] lineValues) {
        change = AddModSupAction.valueOf((String) lineValues[0]);
        versionOfficialDate =  lineValues[1]==null ?null:((Timestamp) lineValues[1]).toLocalDateTime().toLocalDate().toString();
        id = ((Number) lineValues[2]).longValue();
        recordType = RecordType.valueOf((String) lineValues[3]);
        recordReglissReference = (String) lineValues[4];
        akaReglissReferenceRaw = (String) lineValues[5];
        recordId = (String) lineValues[6];
        modElement = AddModSupElementType.valueOf((String) lineValues[7]);
        fullNameRaw = (String) lineValues[8];
        newValueRaw = (String) lineValues[9];
        oldValueRaw = (String) lineValues[10];

        childId = lineValues[11] != null ? Long.parseLong(String.valueOf(lineValues[11])) : null;
        auditedAction =  AuditedAction.valueOf((String) lineValues[12]);
        childType = lineValues[13] != null ? ChildType.valueOf((String) lineValues[13]) : null;
    }

    public String getNewValue() {
        return getValue(newValueRaw);
    }

    public String getOldValue() {
        return getValue(oldValueRaw);
    }

    private String getValue(String valueRaw) {
        if (valueRaw == null) {
            return "";
        }
        switch (modElement) {
        case PRIMARY_NAME:
            return extractFullName(valueRaw);
        case ALIAS_STRONG:
        case ALIAS_WEAK:
        case ALIAS_WEAK_REC:
        case ALIAS_OTHER:
            return extractAliasFullName(valueRaw);
        case ADDRESS:
            return extractAddress(valueRaw);
        case CITIZENSHIP:
            return extractCountryFromCountryDetails(valueRaw);
        default:
            return valueRaw;
        }
    }

    public String getOtherNameAka() {
        if (isAliasModElement()) {
            return getNewValue();
        }
        return "";
    }

    public String getBicCode() {
        if (modElement == AddModSupElementType.BIC) {
            return getNewValue();
        }
        return "";
    }

    public String getIsinCode() {
        if (modElement == AddModSupElementType.ISIN) {
            return getNewValue();
        }
        return "";
    }

    public String getPassportNumber() {
        if (modElement == AddModSupElementType.PASSPORT) {
            return getNewValue();
        }
        return "";
    }

    public String getAkaReglissReference() {
        return getAkaReglissReferenceRaw().map(ar -> ar.substring(recordReglissReference.length()))
                .orElse(StringUtils.EMPTY);
    }

    public String getRecordAliasReference() {
        if (isAliasModElement()) {
            return akaReglissReferenceRaw;
        }
        return recordReglissReference;
    }

    private boolean isAliasModElement() {
        return EnumSet.of(ALIAS_STRONG, ALIAS_WEAK, ALIAS_WEAK_REC, ALIAS_OTHER).contains(modElement);
    }

    private String extractFullName(String raw) {
        String[] splitRaw = splitRaw(raw);
        String firstName = splitRaw[0].trim();
        String middleName = splitRaw[1].trim();
        String lastName = splitRaw[2].trim();
        String fullName = splitRaw[3].trim();
        if (recordType == RecordType.PERSON) {
            return Stream.of(firstName, middleName, lastName.toUpperCase()).filter(StringUtils::isNotBlank)
                    .collect(joining(" ")).trim();
        } else {
            return fullName.trim().toUpperCase();
        }
    }

    private String extractAliasFullName(String raw) {
        String[] splitRaw = splitRaw(raw);
        String firstName = splitRaw[0].trim();
        String middleName = splitRaw[1].trim();
        String lastName = splitRaw[2].trim();
        String fullName = splitRaw[3].trim();
        String suffix = splitRaw[4].trim();
        if (recordType == RecordType.PERSON) {
            return Stream.of(firstName, middleName, lastName.toUpperCase(), suffix).filter(StringUtils::isNotBlank)
                    .collect(joining(" ")).trim();
        } else {
            return fullName.trim().toUpperCase();
        }
    }

    private String extractAddress(String raw) {
        String[] splitRaw = splitRaw(raw);
        String address = splitRaw[0].trim();
        String zipCode = splitRaw[1].trim();
        String city = splitRaw[2].trim();
        String country = extractCountryFromAddress(splitRaw[3].trim());
        return Stream.of(address, zipCode, city, country).filter(StringUtils::isNotBlank)
                .collect(joining(",")).trim();
    }

    private String extractCountryFromAddress(String address) {

        if (!address.contains("=")) {
            // if address is not of key=value type, then do not parse
            return address;
        }

        // the old/new value in Audit table is now generated by toStringForAudit which returns a set of key=label
        // therefore the parameter can look like
        // address=some_address,zipcode=zip_code,city=some_city,country=Country
        Optional<String> countryOpt = Arrays
                .stream(address.split(","))
                .filter(a -> a.contains("country="))
                .findFirst();

        if (countryOpt.isPresent()) {
            return countryOpt.get().split("=")[1];
        }

        // if country does not exist in the complex key=value string, then empty string should be returned for country
        // because country was not actually modified
        return "";
    }

    private String extractCountryFromCountryDetails(String raw) {
        String country = extractCountryFromAddress(raw);
        return StringUtils.isNotBlank(country) ? country : raw;
    }

    private String[] splitRaw(String raw) {
        String padded = " " + raw + " "; // otherwise @@@@@@@@@ leads to 0 items after split !
        return padded.split("@@@");
    }

    public String getRecordReglissReference() {
        return recordReglissReference;
    }

    public String getModElement() {
        return modElement.label;
    }

    public String getModElementNoQuality() {
        return (modElement.label.startsWith(ALIAS)) ? Stream.of(modElement.label.split(" ")).findFirst().orElse("")
                  : modElement.label;
    }

    public String getQualityFromModElement() {
        return (modElement.label.startsWith(ALIAS)) ?  Stream.of(modElement.label.replace("(", "").replace(")", "")
                                                                                .split(" "))
                                                                                .filter(s -> !s.equals(ALIAS))
                                                                                .collect(Collectors.toList()).stream().findAny().orElse("")
                  : "";
    }

    public AddModSupElementType getModElementType() {
        return modElement;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public String getRecordTypeNameForExport() {
        switch (recordType) {
        case PERSON:
            return "Individual";
        case ENTITY:
            return "Entity";
        case VESSEL:
            return "Vessel";
        case AIRPORT:
            return "Airport";
        case CITY:
            return "City";
        case COUNTRY:
            return "Country";
        case OTHER:
            return "Other";
        case SEAPORT:
            return "Seaport";
        default:
            return recordType.name();
        }
    }

    public String getChange() {
        return change.getActionName();
    }

    public AddModSupAction getChangeType() {
        return change;
    }

    public String getVersionOfficialDate() {
        return versionOfficialDate;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getPrimaryName() {
        return extractFullName(fullNameRaw);
    }

    public Optional<String> getAkaReglissReferenceRaw() {
        return Optional.ofNullable(akaReglissReferenceRaw);
    }
}
