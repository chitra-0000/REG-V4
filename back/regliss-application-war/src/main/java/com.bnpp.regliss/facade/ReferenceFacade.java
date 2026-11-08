package com.bnpp.regliss.facade;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.AuditedFieldCode.SemiAutoImportFieldCategory;
import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.ExportFile.GenerationStatus;
import com.bnpp.regliss.entity.FileDelivery.Status;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.record.VerificationStatus;
import com.bnpp.regliss.entity.referential.*;
import com.bnpp.regliss.entity.referential.record.CodeType;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.exporter.Description1FilterValues;
import com.bnpp.regliss.facade.dto.reference.*;
import com.bnpp.regliss.profile.ReglissProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.vo.FeaturePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;

import java.text.Collator;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Facade
@org.springframework.context.annotation.Profile({ReglissProfile.REGLISS_WEB, ReglissProfile.REGLISS_EXTERNAL_API})
public class ReferenceFacade {

    @Value("${is.production.environment}")
    private boolean isProductionEnvironment;

    @Autowired
    private RegulatorOrInternalPolicyRepository regulatorOrInternalPolicyRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ListCategoryRepository listCategoryRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Autowired
    private SanctionTypeRepository sanctionTypeRepo;

    @Autowired
    private IdentityDocumentTypeRepository identityDocumentRepo;

    @Autowired
    private RecordCategoryRepository recordCategoryRepo;

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private GeographicDataRepository geographicDataRepo;

    @Autowired
    private RelationTypeRepository relationTypeRepo;

    @Autowired
    private Description3Repository description3Repo;

    @Autowired
    private PEPOccupationCategoryRepository pepOccupationCategoryRepo;

    @Autowired
    private DateTypeRepository dateTypeRepository;

    @Autowired
    private CountryTypeRepository countryTypeRepository;

    @Autowired
    private GenderRepository genderRepo;

    @Autowired
    private ProgramSanctionTypeRepository programSanctionTypeRepo;

    @Autowired
    private AliasTypeRepository aliasTypeRepo;

    @Autowired
    private AliasQualityRepository aliasQualityRepo;

    @Autowired
    @Lazy
    private ReferenceFacade self;

    public Map<ReferentialType, List<Object>> retrieveUITranslationReferential() {
        Map<ReferentialType, List<Object>> map = new EnumMap<>(ReferentialType.class);
        /*List and Record referential to be deleted and use the method below for them*/

        fillAuditReferential(map);
        fillUITranslationReferential(map);
        return map;
    }

    @Cacheable("listAndRecordReferential")
    public Map<ReferentialType, List<Object>> retrieveListAndRecordReferential() {
        log.debug("Load references from database");
        Map<ReferentialType, List<Object>> map = new EnumMap<>(ReferentialType.class);
        fillListReferential(map);
        fillRecordReferential(map);
        return map;
    }

    @Cacheable("listAndRecordReferentialMap")
    public Map<ReferentialType, Map<String,String>> retrieveListAndRecordReferentialMap() {

        log.debug("Load references Map");
        Map<ReferentialType, List<Object>> refs = self.retrieveListAndRecordReferential();
        Map<ReferentialType,  Map<String,String>> map = new EnumMap<>(ReferentialType.class);

        for(Map.Entry<ReferentialType, List<Object>>refEntry: refs.entrySet()) {
            map.put(
                refEntry.getKey(),
                refEntry.getValue().stream().collect(toMap(
                    e -> String.valueOf(((ReferentialValue) e).getId()),
                    e -> ((ReferentialValue) e).getLabel())
                )
            );
        }
        return map;
    }
    @CacheEvict("listAndRecordReferentialMap")
    public void clearListAndRecordReferentialMap() {
        /*Method used by spring to evict cache of referential*/
    }

    private void fillUITranslationReferential(Map<ReferentialType, List<Object>> map) {
        /*Referential for UI translations*/
        map.put(ReferentialType.objectType, enumToRefs(ObjectType.class));
        map.put(ReferentialType.reglementationType, enumToRefs(Reglementation.ReglementationType.class));
        map.put(ReferentialType.versionStatus, enumToRefs(Status.class));
        map.put(ReferentialType.codeType, enumInTheirOrderToRefs(CodeType.class));
        map.put(ReferentialType.listFormat, enumToRefs(ImportFileType.class));
        map.put(ReferentialType.subfilterOperator, stream(DJSubfilterOperator.values()).map(v-> new EnumReferentialDto(v.name(), v.getValue()))
                .collect(toList()));
        map.put(ReferentialType.mappingFieldCategory, enumInTheirOrderToRefs(SemiAutoImportFieldCategory.class));
        map.put(ReferentialType.watchlistFilterFileExtension,
                asList(enumToStringRef(FileExportExtension.xml), enumToStringRef(FileExportExtension.csv),
                        enumToStringRef(FileExportExtension.zip)));
        map.put(ReferentialType.ameFilterFileExtension, asList(enumToStringRef(FileExportExtension.xml)));
        map.put(ReferentialType.fileFormat, putFircosoftFirst(enumToRefs(FileExportFormat.class)));
        map.put(ReferentialType.fileExtension, enumToRefs(FileExportExtension.class));
        map.put(ReferentialType.fileExportMode, enumToRefs(FileExportMode.class));
        map.put(ReferentialType.filterFileExportMode,
                asList(enumToStringRef(FileExportMode.FULL), enumToStringRef(FileExportMode.ADD_MOD),
                        enumToStringRef(FileExportMode.ADD),enumToStringRef(FileExportMode.MOD),enumToStringRef(FileExportMode.SUPP)));
        map.put(ReferentialType.generationStatus, enumToRefs(GenerationStatus.class));
        map.put(ReferentialType.fileDeliveryStatus, enumToRefs(Status.class));
        map.put(ReferentialType.userStatus, enumToRefs(User.Status.class));
        map.put(ReferentialType.profile, enumToProfileDto(Profile.class));
