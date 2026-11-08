package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.DJSubfilterOperator;
import com.bnpp.regliss.entity.referential.record.*;
import com.bnpp.regliss.exporter.Description1FilterValues;
import com.bnpp.regliss.facade.dto.list.*;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@ReglissWebProfile
public class DJExportFilterMapper {

    @Autowired
    private RecordCategoryRepository recordCategoryRepo;

    @Autowired
    private SanctionTypeRepository sanctionTypeRepo;

    @Autowired
    private Description3Repository description3Repo;

    @Autowired
    private PEPOccupationCategoryRepository pepOccupationCategoryRepo;

    @Autowired
    private CountryRepository countryRepo;

    @Autowired
    private RelationTypeRepository relationTypeRepo;

    @Autowired
    private GenderRepository genderRepository;

    public DJExportFilterAndSubfiltersDto toDJExportFilterAndSubfiltersDto(DJExportFilter filter) {
        DJExportFilterAndSubfiltersDto dto = new DJExportFilterAndSubfiltersDto();
        dto.filter = toDJExportFilterDto(filter);
        dto.subfilters = filter.getSubfilters().stream()
                .filter(DJExportSubfilter::isNotDeleted)
                .sorted(Comparator.comparing(DJExportSubfilter::getIndex))
                .map(this::toDJExportSubfilterDto)
                .collect(toList());
        return dto;
    }

    public DJExportFilterDto toDJExportFilterDto(DJExportFilter filter) {
        DJExportFilterDto filterDto = new DJExportFilterDto();
        filterDto.id =  filter.getId();
        filterDto.filterName = filter.getFilterName();
        filterDto.filterTag = filter.getFilterTag();
        filterDto.extension = filter.getExtensions();
        filterDto.type = filter.getType();
        filterDto.comment = filter.getComment();
        filterDto.isActive = filter.isActive();
        filterDto.generateExportFiles = filter.isGenerateExportFiles();
        return filterDto;
    }

    public DJExportSubfilterDto toDJExportSubfilterDto(DJExportSubfilter subfilter) {
        DJExportSubfilterDto subfilterDto = new DJExportSubfilterDto();
        subfilterDto.id = subfilter.getId();
        subfilterDto.subfilterName = subfilter.getSubfilterName();
        CriteriaDto criteria =new CriteriaDto();
        if(subfilter.getDeathPepSipDateCriteria() != null) {
            OperatorCriteriaDto operatorCriteria= new OperatorCriteriaDto(subfilter.getDeathPepSipDateCriteria().getOperator(),
                    subfilter.getDeathPepSipDateCriteria().getYears());
            criteria.sinceDeathPep = operatorCriteria;
        }else{
            criteria.sinceDeathPep = new OperatorCriteriaDto();
        }
        if(subfilter.getDeathRCADateCriteria() != null) {
            OperatorCriteriaDto operatorCriteria= new OperatorCriteriaDto(subfilter.getDeathRCADateCriteria().getOperator(),
                    subfilter.getDeathRCADateCriteria().getYears());
            criteria.sinceDeathRca = operatorCriteria;

        }else{
            criteria.sinceDeathRca = new OperatorCriteriaDto();
        }
        if(subfilter.getInactivityPepDateCriteria() != null) {
            OperatorCriteriaDto operatorCriteria= new OperatorCriteriaDto(subfilter.getInactivityPepDateCriteria().getOperator(),
                    subfilter.getInactivityPepDateCriteria().getYears());
            criteria.inactivityPep = operatorCriteria;

        }else{
            criteria.inactivityPep = new OperatorCriteriaDto();
        }
        if(subfilter.getInactivityRcaDateCriteria() != null) {
// [MISSING FROM SCREENSHOTS: lines 99-101 of the original file were not visible in the photos]
        }else{
            criteria.inactivityRCA = new OperatorCriteriaDto();
        }
        criteria.description1Ids = toDescription1Ids(subfilter);
        criteria.description2Ids = subfilter.getSanctionTypes().stream().map(SanctionType::getId).collect(Collectors.toList());
        criteria.description3Ids = subfilter.getDescriptions3().stream().map(Description3::getId).collect(Collectors.toList());
        criteria.pepOccupationCategoryIds = subfilter.getPepOccupationCategories().stream().map(PEPOccupationCategory::getId)
                .collect(Collectors.toList());
        Comparator<Country> comparator = Comparator.comparing(e->e.getBusinessConcept()!=null);
        criteria.residenceCountryIds = subfilter.getResidenceCountries().stream().sorted(comparator.reversed()
                .thenComparing(Country::getLabel)).map(Country::getId).collect(Collectors.toList());
        criteria.jurisdictionCountryIds = subfilter.getJurisdictionCountries().stream().sorted(comparator.reversed()
                .thenComparing(Country::getLabel)).map(Country::getId).collect(Collectors.toList());
        criteria.relationTypeIds = subfilter.getRelationTypes().stream().map(RelationType::getId).collect(Collectors.toList());
        criteria.gendersIds=subfilter.getGenders().stream().map(Gender::getId).collect(toList());
        subfilterDto.criteria = criteria;
        subfilterDto.index = subfilter.getIndex();
        return subfilterDto;
    }

    private List<Description1FilterValues> toDescription1Ids(DJExportSubfilter subfilter) {
        if(subfilter.getDescription1() == null) {
            return Arrays.asList(Description1FilterValues.ALL);
        }
        return Arrays.asList(toDescription1FilterValues(subfilter.getDescription1()));
    }

    private Description1FilterValues toDescription1FilterValues(DJSubfilterDescription1 djSubfilterDescription1) {
        if (djSubfilterDescription1 == null) {
            return Description1FilterValues.ALL;
        }

        if (djSubfilterDescription1.getRecordCategory() == null) {
            return Description1FilterValues.ALL;
        }
        String label = djSubfilterDescription1.getRecordCategory().getLabel();
        boolean rca = djSubfilterDescription1.isIncludeRca();
        boolean rcaa = djSubfilterDescription1.isIncludeRcaa();

        if (rca || rcaa) {
            return Description1FilterValues.fromLabelEnAndIncludeRca(label, true, true);
        } else {
            return Description1FilterValues.fromLabelEn(label);
        }
    }

    public DJExportFilter toDJExportFilter(DJExportFilter filter, DJExportFilterDto filterDto, ReglissList list) {
        filter.setFilterName(filterDto.filterName);
        filter.setFilterTag(filterDto.filterTag);
        filter.setExtensions(filterDto.extension);
        filter.setType(filterDto.type);
        filter.setComment(filterDto.comment);
        filter.setFormat(list.getImportConfiguration().getImportFileType());
        filter.setGenerateExportFiles(filterDto.generateExportFiles);
        return filter;
    }

    private DJSubfilterDateCriteria createDjSubfilterDateCriteria(OperatorCriteriaDto operatorCriteriaDto){
        if(operatorCriteriaDto!=null && operatorCriteriaDto.operatorSince != null && operatorCriteriaDto.years != null) {
            return new DJSubfilterDateCriteria(operatorCriteriaDto.operatorSince,operatorCriteriaDto.years);
        }
        return null;
    }

    public DJExportSubfilter toDJExportSubfilter(DJExportSubfilterDto subfilterDto) {
        DJExportSubfilter subfilter = new DJExportSubfilter();
        return this.toDJExportSubfilter(subfilterDto,subfilter);

    }
    public DJExportSubfilter toDJExportSubfilter(DJExportSubfilterDto subfilterDto, DJExportSubfilter subfilter) {
        subfilter.setId(subfilterDto.id);
        subfilter.setSubfilterName(subfilterDto.subfilterName);
        subfilter.setIndex(subfilterDto.index);
        CriteriaDto criteriaDto = subfilterDto.criteria;
        subfilter.setDeathPepSipDateCriteria(createDjSubfilterDateCriteria(criteriaDto.sinceDeathPep));

        subfilter.setDeathRCADateCriteria(createDjSubfilterDateCriteria(criteriaDto.sinceDeathRca));

        subfilter.setInactivityPepDateCriteria(createDjSubfilterDateCriteria(criteriaDto.inactivityPep));

        subfilter.setInactivityRcaDateCriteria(createDjSubfilterDateCriteria(criteriaDto.inactivityRCA));

        for(Description1FilterValues description1FilterValue : criteriaDto.description1Ids) {
            if(description1FilterValue != Description1FilterValues.ALL) {
                RecordCategory recordCategory = recordCategoryRepo.findByLabel(description1FilterValue.toDescription1LabelEn());
                subfilter.setDescription1(recordCategory, description1FilterValue.includeRca(),description1FilterValue.includeRcaa());
            }
            else{
                subfilter.setDescription1(null);
            }

        }
        subfilter.getSanctionTypes().clear();
        for(Long description2Id : criteriaDto.description2Ids.stream().filter(i->i!=null).collect(toList()) )
            subfilter.getSanctionTypes().add(sanctionTypeRepo.getExactlyOne(description2Id));
        subfilter.getDescriptions3().clear();
        for(Long description3Id : criteriaDto.description3Ids.stream().filter(i->i!=null).collect(toList()) )
            subfilter.getDescriptions3().add(description3Repo.getExactlyOne(description3Id));
        subfilter.getPepOccupationCategories().clear();
        for(Long pepOccupationCategoryId : criteriaDto.pepOccupationCategoryIds.stream().filter(i->i!=null).collect(toList()) )
            subfilter.getPepOccupationCategories().add(pepOccupationCategoryRepo.getExactlyOne(pepOccupationCategoryId));
        subfilter.getJurisdictionCountries().clear();
        for(Long jurisdictionCountryId : criteriaDto.jurisdictionCountryIds.stream().filter(i->i!=null).collect(toList()) )
            subfilter.getJurisdictionCountries().add(countryRepo.getExactlyOne(jurisdictionCountryId));
        subfilter.getResidenceCountries().clear();
        for(Long residenceCountryId : criteriaDto.residenceCountryIds.stream().filter(i->i!=null).collect(toList()))
            subfilter.getResidenceCountries().add(countryRepo.getExactlyOne(residenceCountryId));
        subfilter.getRelationTypes().clear();
        for(Long relationTypeId : criteriaDto.relationTypeIds.stream().filter(i->i!=null).collect(toList()))
            subfilter.getRelationTypes().add(relationTypeRepo.getExactlyOne(relationTypeId));
        subfilter.getGenders().clear();
        for(Long genderId : criteriaDto.gendersIds.stream().filter(i->i!=null).collect(toList()))
            subfilter.getGenders().add(genderRepository.getExactlyOne(genderId));
        return subfilter;

    }


}
