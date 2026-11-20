package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.annotation.ImportableChild;
import com.bnpp.regliss.annotation.ImportableReferentialCollection;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ValidationError;
import com.bnpp.regliss.entity.record.*;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.DateType;
import com.bnpp.regliss.entity.referential.record.ProgramSanctionType;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.importer.manual.validation.MappingFileValidator;
import com.bnpp.regliss.importer.manual.validation.RecordValidator;
import com.bnpp.regliss.monitoring.PerformanceMonitor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.DateTypeRepository;
import com.bnpp.regliss.repository.ProgramSanctionTypeRepository;
import com.bnpp.regliss.utils.EntityUtils;
import com.bnpp.regliss.utils.ReflectionCachedUtils;
import com.bnpp.regliss.utils.ReflectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bnpp.regliss.entity.referential.ReferentialBusinessConcept.*;
import static com.bnpp.regliss.utils.ReflectionUtils.getAnnotatedFields;
import static com.bnpp.regliss.utils.ReflectionUtils.getFieldValue;
import static java.util.Collections.singletonList;

@Service
@ReglissBatchProfile
@Scope("prototype")
public class RecordParser {

    private static final Logger log = LoggerFactory.getLogger(RecordParser.class);

    @Autowired
    private RecordFieldResolver resolver;

    @Autowired
    private MappingFileValidator mappingFileValidator;

    @Autowired
    private ProgramSanctionTypeRepository programSanctionTypeRepository;

    @Autowired
    private DateTypeRepository dateTypeRepo;

    private Map<AuditedFieldCode, Function<String, Object>> customFieldMappers; // NOSONAR for performance
    private RecordValidator validator; // NOSONAR for performance
    private RecordFieldIndexes indexes; // NOSONAR for performance
    private Map<ReferentialBusinessConcept, RecordReferential> referentialsUsedInParsing = new EnumMap<>(ReferentialBusinessConcept.class); // NOS

    public void setResolver(RecordFieldResolver resolver) { this.resolver = resolver; }

    public void initialize(MappingFile mapping) {
        customFieldMappers = resolver.getCustomFieldMapper();
        indexes = new RecordFieldIndexes(mapping);
        validator = mappingFileValidator.createRecordValidator(customFieldMappers, indexes);

        referentialsUsedInParsing.putAll(dateTypeRepo.findByBusinessConceptAndV3Only(ReferentialBusinessConcept.DATE_TYPE_DATE_OF_BIRTH,
                        false).stream()
                .collect(Collectors.toMap(RecordReferential::getBusinessConcept, s -> s, (s1, s2) -> s1)));
        referentialsUsedInParsing.putAll(Stream.concat(
                programSanctionTypeRepository.findByBusinessConcept(ReferentialBusinessConcept.PROGRAM_TYPE_EU_SANCTIONS_PROGRAME_INDICATOR)
                        .stream(),
                programSanctionTypeRepository.findByBusinessConcept(ReferentialBusinessConcept.PROGRAM_TYPE_OFAC_PROGRAM_ID).stream())
            .collect(Collectors.toMap(RecordReferential::getBusinessConcept, s -> s, (s1, s2) -> s1)));
    }

    public void setMappingFileValidator(MappingFileValidator mappingFileValidator) {
        this.mappingFileValidator = mappingFileValidator;
    }

    public RecordTry<Record> createRecord(MappingFile mapping, ParsedLine row) {
        try {
            if (customFieldMappers == null) {
                throw new IllegalStateException("Must call initialize() before on this instance");
            }
            List<ValidationError> errors = new ArrayList<>();
            errors.addAll(normalizeRecordType(row.cells));
            PerformanceMonitor.before(PerformanceMonitor.Action.VALIDATE_RECORD);
            errors.addAll(validator.validateRecord(mapping, row.cells));
            PerformanceMonitor.after(PerformanceMonitor.Action.VALIDATE_RECORD);

            if (!errors.isEmpty()) {
                return new RecordTry<>(new LineImportErrors(row.lineNumber, errors));
            }
            Record record = instantiateRecord(row);

            PerformanceMonitor.before(PerformanceMonitor.Action.FILL_RECORD_ATTRIBUTES);
            fillRecordAttributes(record, indexes.forRecord(), row.cells);
            PerformanceMonitor.after(PerformanceMonitor.Action.FILL_RECORD_ATTRIBUTES);

            PerformanceMonitor.before(PerformanceMonitor.Action.FILL_LABEL_COLLECTIONS);
            fillLabelCollections(record, row.cells);
            PerformanceMonitor.after(PerformanceMonitor.Action.FILL_LABEL_COLLECTIONS);

            PerformanceMonitor.before(PerformanceMonitor.Action.FILL_CHILDREN);
            fillChildren(record, row.cells);
            PerformanceMonitor.after(PerformanceMonitor.Action.FILL_CHILDREN);

            fillAdditionalFields(record);

            return new RecordTry<>(record);

        } catch (Exception e) {
            log.error("Fatal exception for the entire row: " + e, e);
            List<ValidationError> errors = singletonList(new ValidationError(ErrorCode.GENERAL));
            return new RecordTry<>(new LineImportErrors(row.lineNumber, errors));
        }
    }

    private void fillAdditionalFields(Record record) {
        if(record instanceof PersonRecord){
            PersonRecord person = (PersonRecord) record;
            if(person.getLastName() == null) {
                person.setLastName(person.getName());
            }
            String computedFullName = Stream.of(person.getLastName(), person.getMiddleName(),person.getFirstName())
                    .filter(StringUtils::isNotEmpty)
                    .map(StringUtils::trim)
                    .collect(Collectors.joining(" "));
            if(StringUtils.isNotBlank(computedFullName)) {
                record.setName(computedFullName);
            }
        }
    }

    private List<ValidationError> normalizeRecordType(List<String> row) {
        Function<String, ?> recordTypeResolver = customFieldMappers.get(AuditedFieldCode.RECORD_TYPE);
        int recordTypeColumnIndex = indexes.forRecord().getSingleIndexForField(AuditedFieldCode.RECORD_TYPE);
        String rawRecordTypeStr = row.get(recordTypeColumnIndex);
        List<ValidationError> errors = new ArrayList<>();
        RecordType recordType = (RecordType) resolveFieldCollectingErrors(rawRecordTypeStr, recordTypeResolver, errors);
        String recordTypeStr = recordType != null ? recordType.name() : null;
        row.set(recordTypeColumnIndex, recordTypeStr);
        return errors;
    }

    private Object resolveFieldCollectingErrors(String columnValue, Function<String, ?> fieldResolver, List<ValidationError> errors) {
        try {
            return fieldResolver.apply(columnValue);
        } catch (ReglissException e) {
            errors.add(new ValidationError(e.getErrorCode(), e.getParameters()));
            return null;
        }
    }

    private Record instantiateRecord(ParsedLine row) {
        String typeStr = indexes.forRecord().getSingleColumnValue(AuditedFieldCode.RECORD_TYPE, row.cells);
        return RecordType.valueOf(typeStr).instantiate();
    }

    private void fillChildren(Record record, List<String> row) {
        Object lastProgram = null;
        for (Field collectionField : ReflectionCachedUtils.getAnnotatedFields(record.getClass(), ImportableChild.class)) {
            Class<?> childClass = ReflectionCachedUtils.getElementType(collectionField);
            ChildType childType = collectionField.getAnnotation(ImportableChild.class).type();

            Method adder = ReflectionCachedUtils.getChildAddMethod(record.getClass(), childClass);

            for (EntityFieldIndexes fieldIndexes : indexes.forChild(childType)) {
                log.trace("Filling child {}", childClass.getSimpleName());
                List<AbstractSimpleEntity> children = fillChildrenOfType(childType, childClass, fieldIndexes, row, record.getType());

                for (AbstractSimpleEntity child : children) {
                    ReflectionUtils.invoke(record, adder, child);
                }
                if(childType == ChildType.PROGRAM && !children.isEmpty()) {
                    lastProgram = children.iterator().next();
                }
            }
        }
        if(lastProgram != null) {
            ProgramSanctionType lastProgramType = (ProgramSanctionType) getReferentialForBusinessConcept(PROGRAM_TYPE_OFAC_PROGRAM_ID);
            ((Program)lastProgram).setSanctionType(lastProgramType);
        }
    }

    private List<AbstractSimpleEntity> fillChildrenOfType(ChildType childType, Class<?> childClass, EntityFieldIndexes fieldIndexes,
            List<String> row, RecordType recordType){
        switch(childType) {
        case  BIC:
            return fillBicCodes(childClass, fieldIndexes, row);
        case PERSON_BIOGRAPHY:
            return fillPersonBiography(childClass, fieldIndexes, row)
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList());
        case PROGRAM:
            return fillProgram(childClass, fieldIndexes, row)
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList());
        case ALIAS:
            return fillAlias(childClass, fieldIndexes, row, recordType)
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList());
        default:
            return fillChildAttributes(childClass, fieldIndexes, row)
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList());
        }
    }

    private Optional<AbstractSimpleEntity> fillProgram(Class<?> childClass, EntityFieldIndexes fieldIndexes,
            List<String> row) {
        Optional<AbstractSimpleEntity> programOpt = fillChildAttributes(childClass, fieldIndexes, row);
        if(programOpt.isPresent()) {
            Program program = (Program) programOpt.get();
            ProgramSanctionType defaultProgramType = (ProgramSanctionType) getReferentialForBusinessConcept(
                    PROGRAM_TYPE_EU_SANCTIONS_PROGRAME_INDICATOR);
            program.setSanctionType(defaultProgramType);
            return Optional.of(program);
        }
        return programOpt;
    }

    private Optional<AbstractSimpleEntity> fillAlias(Class<?> childClass, EntityFieldIndexes fieldIndexes,
            List<String> row, RecordType recordType) {
        Optional<AbstractSimpleEntity> aliasOpt = fillChildAttributes(childClass, fieldIndexes, row);
        if(aliasOpt.isPresent()) {
            Alias alias = (Alias) aliasOpt.get();
            if(recordType == RecordType.PERSON) {
                if(alias.getLastName() != null) {
                    alias.setFullName(alias.getLastName());
                } else if(alias.getFullName() != null) {
                    alias.setLastName(alias.getFullName());
                }
            } else {
                if(alias.getFullName() != null) {
                    alias.setLastName(alias.getFullName());
                } else if(alias.getLastName() != null) {
                    alias.setFullName(alias.getLastName());
                }
                alias.setFirstName(null);
                alias.setMiddleName(null);
            }

            return Optional.of(alias);
        }
        return aliasOpt;
    }

    private void fillRecordAttributes(Object object, EntityFieldIndexes entityFieldIndexes, List<String> row) {
        PerformanceMonitor.before(PerformanceMonitor.Action.FILL_ATTRIBUTES);

        for (Field field : ReflectionCachedUtils.getAnnotatedFields(object.getClass(), AuditedField.class)) {
            AuditedFieldCode fieldCode = field.getAnnotation(AuditedField.class).value();
            PerformanceMonitor.before(PerformanceMonitor.Action.GET_COLUMN_VALUE);
            String stringValue = entityFieldIndexes.getJoinedColumnValues(fieldCode, row);
            PerformanceMonitor.after(PerformanceMonitor.Action.GET_COLUMN_VALUE);
            Object resolvedValue = getResolvedValue(fieldCode, stringValue);
            fillAttribute(object, resolvedValue, field, fieldCode);
        }
        PerformanceMonitor.after(PerformanceMonitor.Action.FILL_ATTRIBUTES);
    }

    private Optional<AbstractSimpleEntity> fillChildAttributes(Class<?> childClass, EntityFieldIndexes entityFieldIndexes,
                                                               List<String> row) {
        PerformanceMonitor.before(PerformanceMonitor.Action.FILL_CHILDREN);

        AbstractSimpleEntity newChild = (AbstractSimpleEntity) ReflectionUtils.newInstance(childClass);
        boolean childHasNonEmptyFields = false;
        for (Field field : ReflectionCachedUtils.getAnnotatedFields(childClass, AuditedField.class)) {
            AuditedFieldCode fieldCode = field.getAnnotation(AuditedField.class).value();
            PerformanceMonitor.before(PerformanceMonitor.Action.GET_COLUMN_VALUE);
            String stringValue = entityFieldIndexes.getJoinedColumnValues(fieldCode, row);
            if (String.class.equals(field.getType())) {
                int maxLength = field.getAnnotation(Column.class).length();
                if (stringValue != null && maxLength>0 &&  stringValue.getBytes(StandardCharsets.UTF_8).length > maxLength) {
                    stringValue = EntityUtils.truncateUtf8(stringValue, maxLength);
                }
            }
            PerformanceMonitor.after(PerformanceMonitor.Action.GET_COLUMN_VALUE);
            if(stringValue != null) {
                childHasNonEmptyFields = true;
            }
            Object resolvedValue = getResolvedValue(fieldCode, stringValue);
            fillAttribute(newChild, resolvedValue, field, fieldCode);
        }
        PerformanceMonitor.after(PerformanceMonitor.Action.FILL_CHILDREN);

        if(childHasNonEmptyFields) {
            return Optional.of(newChild);
        }
        return Optional.empty();
    }

    private List<AbstractSimpleEntity> fillBicCodes(Class<?> childClass, EntityFieldIndexes entityFieldIndexes, List<String> row) {
        PerformanceMonitor.before(PerformanceMonitor.Action.FILL_CHILDREN);

        List<AbstractSimpleEntity> children = new ArrayList<>();
        for (Field field : ReflectionCachedUtils.getAnnotatedFields(childClass, AuditedField.class)) {
            AuditedFieldCode fieldCode = field.getAnnotation(AuditedField.class).value();
            PerformanceMonitor.before(PerformanceMonitor.Action.GET_COLUMN_VALUE);
            String stringValue = entityFieldIndexes.getJoinedColumnValues(fieldCode, row);
            PerformanceMonitor.after(PerformanceMonitor.Action.GET_COLUMN_VALUE);
            Object resolvedValue = getResolvedValue(fieldCode, stringValue);
            if(resolvedValue != null) {
                for (String value : ((String)resolvedValue).split(" ")) {
                    AbstractSimpleEntity child = (AbstractSimpleEntity) ReflectionUtils.newInstance(childClass);
                    fillAttribute(child, value, field, fieldCode);
                    children.add(child);
                }
            }
        }
        PerformanceMonitor.after(PerformanceMonitor.Action.FILL_CHILDREN);
        return children;
    }

    private Optional<AbstractSimpleEntity> fillPersonBiography(Class<?> childClass, EntityFieldIndexes entityFieldIndexes, List<String> row) {
        Optional<AbstractSimpleEntity> biographyOpt = fillChildAttributes(childClass, entityFieldIndexes, row);

        if(biographyOpt.isPresent()) {
            PersonBiography personBiography = (PersonBiography)biographyOpt.get();
            if(personBiography.getDateValueStrOpt().isPresent()) {
                DateType biographyDefaultDateType = (DateType) getReferentialForBusinessConcept(DATE_TYPE_DATE_OF_BIRTH);
                personBiography.setDateType(biographyDefaultDateType);
                return Optional.of(personBiography);
            }
        }
        return biographyOpt;
    }

    private void fillAttribute(Object object, Object resolvedValue, Field field, AuditedFieldCode fieldCode) {
        if(resolvedValue != null) {
            log.trace("Assigning field {} to value {}", fieldCode, resolvedValue);
            Method setter = ReflectionCachedUtils.getSetter(field);
            ReflectionUtils.invoke(object, setter, resolvedValue);
        }
    }

    private void fillLabelCollections(Record record, List<String> row) {
        for (Field collectionField : getAnnotatedFields(record.getClass(), ImportableReferentialCollection.class)) {
            AuditedFieldCode fieldCode = collectionField.getAnnotation(ImportableReferentialCollection.class).code();

            List<String> allColumnValues = indexes.forRecord().getAllColumnValues(fieldCode, row);

            Collection<Object> labelCollection = getFieldValue(collectionField, record);

            for (String stringValue : allColumnValues) {
                if (StringUtils.isNotBlank(stringValue)) {
                    Object value = getResolvedValue(fieldCode, stringValue);
                    if(value != null) {
                        log.trace("Adding to label collection field {} the label {}", fieldCode, value);
                        labelCollection.add(value);
                    }
                }
            }
        }
    }

    private Object getResolvedValue(AuditedFieldCode fieldCode, String stringValue) {
        PerformanceMonitor.before(PerformanceMonitor.Action.GET_MAPPINGS);
        Function<String, ?> customFieldMapper = customFieldMappers.get(fieldCode);
        PerformanceMonitor.after(PerformanceMonitor.Action.GET_MAPPINGS);
        if (customFieldMapper != null) {
            return customFieldMapper.apply(stringValue);
        } else {
            return stringValue;
        }
    }

    private RecordReferential getReferentialForBusinessConcept(ReferentialBusinessConcept businessConcept) {
        RecordReferential recordReferential = referentialsUsedInParsing.get(businessConcept);
        if(recordReferential == null) {
            log.error("The value with business concept {} not found in the known referentials", businessConcept);
        }
        return recordReferential;
    }

}

