package com.bnpp.regliss.importer.dj.referential.db;

// [MISSING FROM SCREENSHOTS: lines 3-18 of the original file were not visible in the photos]

@Component
@ReglissBatchProfile
@Slf4j
public class AutoDbReferentialsExtractor {
    @Autowired
    private Description3Repository description3Repo;
    @Autowired
    private SanctionTypeRepository sanctionTypeRepo;
    @Autowired
    private PEPOccupationCategoryRepository occupationRepo;
    @Autowired
    private DateTypeRepository dateTypeRepo;
    @Autowired
    private CountryTypeRepository countryTypeRepo;
    @Autowired
    private IdentityDocumentTypeRepository identityDocumentTypeRepo;
    @Autowired
    private ProgramSanctionTypeRepository programSanctionTypeRepo;
    @Autowired
    private RecordCategoryRepository recordCategoryRepo;
    @Autowired
    private CountryRepository countryRepo;
    @Autowired
    private GenderRepository genderRepo;
    @Autowired
    private AliasTypeRepository aliasTypeRepo;
    @Autowired
    private AliasQualityRepository aliasQualityRepo;
    @Autowired
    private RelationTypeRepository relationTypeRepository;

    public DbReferentials getReferentials(ImportFileType format) {
        //TODO remove code after migration
        log.info("Loading from DB");
        boolean includeV3Referentials=false;
        if(format.isCustomFileType()){
            includeV3Referentials=true;
        }
        DbReferentials referentials = new DbReferentials();

        referentials.set(DJReferentialType.COUNTRY_TYPE, mapLabelToId(countryTypeRepo.findAll(), includeV3Referentials));
        referentials.set(DJReferentialType.PROGRAM_SANCTION_TYPE, mapLabelToId(programSanctionTypeRepo.findAll(), includeV3Referentials));
        referentials.set(DJReferentialType.IDENTITY_DOCUMENT_TYPE, mapLabelToId(identityDocumentTypeRepo.findAll(), includeV3Referentials));
        referentials.set(DJReferentialType.GENDER, mapLabelToId(genderRepo.findAll(), includeV3Referentials));
        referentials.set(DJReferentialType.RECORD_CATEGORY, mapLabelToId(recordCategoryRepo.findAll(), includeV3Referentials));
        referentials.set(DJReferentialType.SANCTION_TYPE, mapLabelWithParentToId(sanctionTypeRepo.findAll(), includeV3Referentials));
        referentials.set(DJReferentialType.DESCRIPTION3, mapLabelWithParentToId(description3Repo.findAll(), includeV3Referentials));
        referentials.set(DJReferentialType.COUNTRY, mapLabelToId(countryRepo.findAll(), includeV3Referentials));
        referentials.set(DJReferentialType.PEP_OCCUPATION_CATEGORY, mapLabelToId(occupationRepo.findAll(), includeV3Referentials));
        if(includeV3Referentials){
            referentials.set(DJReferentialType.ALIAS_TYPE, mapLabelToId(aliasTypeRepo.findAll(), true));
            referentials.set(DJReferentialType.ALIAS_QUALITY, mapLabelToId(aliasQualityRepo.findAll(), true));
            referentials.set(DJReferentialType.DATE_TYPE, mapLabelAndBusinessConceptToId(dateTypeRepo.findAll(), includeV3Referentials));
        }else {
            referentials.set(DJReferentialType.ALIAS_TYPE, mapBusinessConceptToId(aliasTypeRepo.findAll(), includeV3Referentials));
            referentials.set(DJReferentialType.ALIAS_QUALITY, mapBusinessConceptToId(aliasQualityRepo.findAll(), includeV3Referentials));
            referentials.set(DJReferentialType.DATE_TYPE, mapLabelToId(dateTypeRepo.findAll(), includeV3Referentials));
        }
        referentials.set(DJReferentialType.RELATION_TYPE, mapLabelToId(relationTypeRepository.findAll(), includeV3Referentials));
        return referentials;
    }

    private Map<String, Long> mapLabelToId(List<? extends RecordReferential> all, boolean includeV3Referentials) {
        Map<String, Long> result = new HashMap<>();
        List<? extends RecordReferential> referentialsToBeIncluded = filterReferentialsToBeIncluded(all, includeV3Referentials);

        List<? extends RecordReferential> defaultValues = referentialsToBeIncluded.stream()
                .filter(r -> r.hasBusinessConcept(ReferentialBusinessConcept.DEFAULT_VALUE))
                .collect(toList());
        result.putAll(defaultValues.stream().collect(toMap(l -> ReferentialBusinessConcept.DEFAULT_VALUE.name(), RecordReferential::getId,
                (x, y) -> x)));

        List<? extends RecordReferential> referentialsNotDefault = ListUtils.removeAll(referentialsToBeIncluded, defaultValues);
        result.putAll(referentialsNotDefault.stream().collect(toMap(l -> l.getLabel().trim(), RecordReferential::getId, (x, y) -> x)));

        return result;
    }

    private Map<String, Long> mapLabelAndBusinessConceptToId(List<? extends RecordReferential> all, boolean includeV3Referentials) {
        Map<String, Long> result = new HashMap<>();
        List<? extends RecordReferential> referentialsToBeIncluded = filterReferentialsToBeIncluded(all, includeV3Referentials);

        List<? extends RecordReferential> defaultValues = referentialsToBeIncluded.stream()
                .filter(r -> r.hasBusinessConcept(ReferentialBusinessConcept.DEFAULT_VALUE))
                .collect(toList());
        result.putAll(defaultValues.stream().collect(toMap(l -> ReferentialBusinessConcept.DEFAULT_VALUE.name(), RecordReferential::getId,
                (x, y) -> x)));

        List<? extends RecordReferential> referentialsNotDefault = ListUtils.removeAll(referentialsToBeIncluded, defaultValues);
        result.putAll(referentialsNotDefault.stream().collect(toMap(l -> l.getLabel().trim()+ "+"+l.getBusinessConcept().get().name(),
                RecordReferential::getId, (x, y) -> x)));

        return result;
    }

    private Map<String, Long> mapBusinessConceptToId(List<? extends RecordReferential> all, boolean includeV3Referentials) {
        List<? extends RecordReferential> referentialsToBeIncluded = filterReferentialsToBeIncluded(all, includeV3Referentials);
        return referentialsToBeIncluded.stream()
                .filter(r -> r.getBusinessConceptOpt().isPresent())
                .collect(toMap(l -> l.getBusinessConceptOpt().get().name(), RecordReferential::getId, (x, y) -> x));
    }

    private Map<String, Long> mapLabelWithParentToId(List<? extends Description> all, boolean includeV3Referentials) {
        Map<String, Long> result = new HashMap<>();
        List<? extends Description> referentialsToBeIncluded =
                (List<? extends Description>) filterReferentialsToBeIncluded(all, includeV3Referentials);

        List<? extends Description> defaultValues = referentialsToBeIncluded.stream()
                .filter(r -> r.hasBusinessConcept(ReferentialBusinessConcept.DEFAULT_VALUE)).collect(toList());
        result.putAll(defaultValues.stream().collect(toMap(l -> ReferentialBusinessConcept.DEFAULT_VALUE.name(), Description::getId,
                (x, y) -> x)));

        List<? extends Description> referentialsNotDefault = ListUtils.removeAll(referentialsToBeIncluded, defaultValues);
        result.putAll(referentialsNotDefault.stream().collect(toMap(Description::computeBusinessKey, Description::getId, (x, y) -> x)));

        return result;
    }

    private List<? extends RecordReferential> filterReferentialsToBeIncluded(List<? extends RecordReferential> all,
                                                                             boolean includeV3Referentials) {
        return all.stream()
                .filter(r -> referentialShouldBeIncluded(r, includeV3Referentials))
                .collect(toList());
    }

    private boolean referentialShouldBeIncluded(RecordReferential r, boolean includeV3Referentials) {
        if(includeV3Referentials){
            return true;
        }
        return r.isV3Only() == false;
    }

}
