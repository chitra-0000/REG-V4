package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.record.PersonRecord;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.Scope;
import com.bnpp.regliss.entity.referential.WatchlistBroadcastFrequency;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.facade.dto.AuditDto;
import com.bnpp.regliss.facade.dto.VersionAuditDto;
import com.bnpp.regliss.facade.dto.record.RecordAuditDto;
import com.bnpp.regliss.facade.dto.version.RecordMinorChangesDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.service.ReferenceService;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.LongFunction;

@Service
@ReglissWebProfile
public class AuditMapper {
    @Autowired
    private ReferenceService referenceService;

    private static final String DECEASED_DATE = "dateType=Deceased Date";
    private static final String DATE_OF_REGISTRATION = "dateType=Date of Registration";
    private static final String DATE_OF_CESSATION = "dateType=Cessation Date";
    private static final String INACTIVE_PEP = "dateType=Inactive as of (PEP)";
    private static final String INACTIVE_RCA_RELATED_TO_PEP = "dateType=Inactive as of (RCA related to PEP)";
    private static final String BIRTH_PLACE = "birthCountry";

    public AuditDto toAuditDto(Audit entity) {
        AuditDto dto = new AuditDto();
        fillCommonAuditInfo(dto, entity);
        dto.objectType = entity.getObjectType();
        return dto;
    }

    private void fillCommonAuditInfo(AuditDto dto, Audit entity) {
        dto.auditedAction = entity.getAuditedAction();
        dto.auditedField = entity.getAuditedField();
        dto.logDate = entity.getLogDate();
        dto.newValue = resolveName(entity.getAuditedField(), entity.getNewValue());
        dto.oldValue = resolveName(entity.getAuditedField(), entity.getOldValue());
        dto.reference = entity.getReference();
        dto.username = entity.getUser().getUsername();
    }

    private String resolveName(AuditedFieldCode auditedField, String fieldValue) {
        if (auditedField == null) {
            return fieldValue;
        }
        if(fieldValue == null){
            return "";
        }
        try {
            switch (auditedField) {
                case LIST_REGULATOR_IP:
                    return referenceService.getRegulatorOrInternalPolicyById(Long.parseLong(fieldValue)).getLabel();
                case LIST_CONFIDENTIALITY:
                    return Confidentiality.valueOf(fieldValue).getLabel();
                case LIST_CATEGORY:
                    return referenceService.getCategoryById(Long.parseLong(fieldValue)).getLabel();
                case LIST_ISSUER:
                    return referenceService.getIssuerById(Long.parseLong(fieldValue)).getLabel();
                case LIST_SCOPE:
                    return referenceService.translateEnum(Scope.valueOf(fieldValue));
                case LIST_TYPE:
                    return ReglissListType.valueOf(fieldValue).getLabel();
                case LIST_GEOGRAPHIC_DATA:
                    return referenceService.getGeographicDataById(Long.parseLong(fieldValue)).getLabel();
                case LIST_IMPORT_FILE_TYPE:
                    return referenceService.translateEnum(ImportFileType.valueOf(fieldValue));
                case RECORD_SANCTION_TYPE:
                    return getDetailsFromDbIfFieldValueIsId(fieldValue, referenceService::getSanctionTypeById);
                case RECORD_CATEGORY:
                    return getDetailsFromDbIfFieldValueIsId(fieldValue, referenceService::getRecordCategory);
                case RECORD_ALIAS_TYPE:
                    return getDetailsFromDbIfFieldValueIsId(fieldValue, referenceService::getAliasTypeById);
                case RECORD_ALIAS_QUALITY:
                    return getDetailsFromDbIfFieldValueIsId(fieldValue, referenceService::getAliasQualityById);
                case RECORD_ADDRESS_COUNTRY:
                case RECORD_COUNTRY_DETAILS_BIRTH_COUNTRY:
                case LIST_COUNTRY:
                case RECORD_PERSON_BIOGRAPHY_BIRTH_COUNTRY:
                case SUBSCRIBER_COUNTRY:
                case RECORD_IDENTITY_DOCUMENT_COUNTRY:
                    return getDetailsFromDbIfFieldValueIsId(fieldValue, referenceService::getCountryById);
                case RECORD_PERSON_BIOGRAPHY_DATE_TYPE:
                case RECORD_DATE_DETAILS_DATE_TYPE:
                    return getDetailsFromDbIfFieldValueIsId(fieldValue, referenceService::getDateTypeById);
                case RECORD_IDENTITY_DOCUMENT_TYPE:
                    return getDetailsFromDbIfFieldValueIsId(fieldValue, referenceService::getIdentityDocumentTypeById);
                case RECORD_COUNTRY_DETAILS_TYPE:
                    return getDetailsFromDbIfFieldValueIsId(fieldValue, referenceService::getCountryTypeById);
                case RECORD_RELATION_TYPE:
                    return getDetailsFromDbIfFieldValueIsId(fieldValue, referenceService::getRelationTypeById);
                case USER_PROFILE:
                    return referenceService.translateEnum(Profile.valueOf(fieldValue));
                case USER_LANGUAGE:
                    return referenceService.translateEnum(Language.valueOf(fieldValue));
                case USER_PERMISSION:
                    return referenceService.translateEnum(ListPermission.valueOf(fieldValue));
                case SUBSCRIBER_WL_FULL_BROADCAST_FREQUENCY:
                    return referenceService.translateEnum(WatchlistBroadcastFrequency.valueOf(fieldValue));
                case SUBSCRIBER_WL_DELTA_BROADCAST_FREQUENCY:
                    return referenceService.translateEnum(WatchlistBroadcastFrequency.valueOf(fieldValue));

                case RECORD_PROGRAM_SANCTION_TYPE:
                case USER_UID:
                case USER_FIRST_NAME:
                case USER_LAST_NAME:
                case USER_EMAIL:
                case USER_ENTITY:
                case USER_POLE:
                case USER_COUNTRY_NAME:
                case USER_BUSINESS_LINE:
                default:
                    return fieldValue;
            }
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getDetailsFromDbIfFieldValueIsId(String fieldValue, LongFunction<RecordReferential> repositoryFunction) {
        if (NumberUtils.isParsable(fieldValue)) {
            // handle old IDs that existed in Audit table before changing the RecordReference.toStringAudit()
            return repositoryFunction.apply(Long.parseLong(fieldValue)).getLabel();
        } else {
            return fieldValue;
        }
    }

    public VersionAuditDto toVersionAuditDto(Audit entity) {
        VersionAuditDto dto = new VersionAuditDto();
        dto.auditedAction = entity.getAuditedAction();
        dto.logDate = entity.getLogDate();
        dto.username = entity.getUser().getUsername();
        dto.prevValue = entity.getOldValue();
        dto.newValue = entity.getNewValue();
        dto.auditedField = entity.getAuditedField();
        if (dto.auditedAction.equals(AuditedAction.VERSION_RETURN_TO_VALIDATION)
                || dto.auditedAction.equals(AuditedAction.VERSION_VERIFICATION)
                || dto.auditedAction.equals(AuditedAction.VERSION_CONFIRMATION)) {
            dto.returnComment = entity.getNewValue();
        }
        return dto;
    }

    public RecordAuditDto toRecordAuditDto(Audit audit, Record record) {
        Long childId = audit.getObjectId();
        RecordAuditDto dto = new RecordAuditDto();
        fillCommonAuditInfo(dto, audit);
        dto.objectType = (childId != null) ? ObjectType.RELATION : ObjectType.RECORD;
        dto.childId = childId;
        dto.listId = audit.getVersion().getListId();
        dto.versionNumber = audit.getVersion().getVersionNumber();
        dto.versionId = audit.getVersion().getId();
        dto.name = mapRecordName(audit,record);
        return dto;
    }

    public RecordMinorChangesDto toRecordMinorUpdateDTO(Audit audit, Record record) {
        RecordMinorChangesDto recordMinorChangesDto = new RecordMinorChangesDto();
        recordMinorChangesDto.recordType = (record != null) ? referenceService.translateEnum(record.getType()) : null;
        recordMinorChangesDto.recordReglissReference = audit.getReference();
        recordMinorChangesDto.primaryName = mapRecordName(audit, record);
        recordMinorChangesDto.newValue = audit.getNewValue();
        recordMinorChangesDto.oldValue = audit.getOldValue();
        recordMinorChangesDto.auditedFieldCode = (audit.getAuditedField() != null && audit.getAuditedField().equals(
                AuditedFieldCode.RECORD_ADDRESS_ZIP_CODE)) ? null : audit.getAuditedField();
        ChildType childType = audit.getAuditedField() != null ? audit.getAuditedField().getChildType() : audit.getAuditedAction()
                .getChildType();

        if (childType != null) {
            if (childType.equals(ChildType.PERSON_BIOGRAPHY)) {
                setRecordDtoChildTypeBasedOnDates(audit, recordMinorChangesDto);
                setRecordDtoChildType(audit, recordMinorChangesDto, childType);
            } else {
                recordMinorChangesDto.childType = (childType.equals(ChildType.ADDRESS)) ? referenceService
                        .translateValue("ChildType.ADDRESS_ZIP_ADDRESS") : referenceService.translateEnum(childType);
            }
        }
        recordMinorChangesDto.externalUuid = (record != null) ? record.getExternalUuid() : null;

        return recordMinorChangesDto;
    }

    private String mapRecordName(Audit audit, Record record) {
        return NameMappingUtil.toUppercaseLastName(audit.getRecordName(),record);
    }

    private void setRecordDtoChildType(Audit audit, RecordMinorChangesDto recordMinorChangesDto, ChildType childType) {
        if ((audit.getNewValue() != null && audit.getNewValue().startsWith(INACTIVE_PEP)) || audit.getOldValue() != null &&
                audit.getOldValue().startsWith(INACTIVE_PEP)) {
            recordMinorChangesDto.childType = referenceService.translateValue("ChildType.INACTIVE_PEP");
        }else if ((audit.getNewValue() != null && audit.getNewValue().startsWith(INACTIVE_RCA_RELATED_TO_PEP)) || audit.getOldValue() != null
                && audit.getOldValue().startsWith(INACTIVE_RCA_RELATED_TO_PEP)) {
            recordMinorChangesDto.childType = referenceService.translateValue("ChildType.INACTIVE_RCA_RELATED_TO_PEP");
        }else if ((audit.getNewValue() != null && audit.getNewValue().startsWith(BIRTH_PLACE)) || audit.getOldValue() != null
                && audit.getOldValue().startsWith(BIRTH_PLACE)) {
            recordMinorChangesDto.childType = referenceService.translateEnum(childType);
        }
    }

    private void setRecordDtoChildTypeBasedOnDates(Audit audit, RecordMinorChangesDto recordMinorChangesDto) {
        if ((audit.getNewValue() != null && audit.getNewValue().startsWith(DECEASED_DATE)) || audit.getOldValue() != null
                && audit.getOldValue().startsWith(DECEASED_DATE)) {
            recordMinorChangesDto.childType = referenceService.translateValue("ChildType.DATE_OF_DEATH");
        } else if ((audit.getNewValue() != null && audit.getNewValue().startsWith(DATE_OF_REGISTRATION)) || audit.getOldValue() != null
                && audit.getOldValue().startsWith(DATE_OF_REGISTRATION)) {
            recordMinorChangesDto.childType = referenceService.translateValue("ChildType.DATE_OF_REGISTRATION");
        }else if ((audit.getNewValue() != null && audit.getNewValue().startsWith(DATE_OF_CESSATION)) || audit.getOldValue() != null
                && audit.getOldValue().startsWith(DATE_OF_CESSATION)) {
            recordMinorChangesDto.childType = referenceService.translateValue("ChildType.DATE_OF_CESSATION");
        }
    }
}
