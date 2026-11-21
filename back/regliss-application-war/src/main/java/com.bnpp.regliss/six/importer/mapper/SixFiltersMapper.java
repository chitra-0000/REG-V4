package com.bnpp.regliss.six.importer.mapper;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.entity.six.entity.SixFilters;
import com.bnpp.regliss.entity.six.entity.SixSubFilters;
import com.bnpp.regliss.entity.six.entity.SixSubORFilters;
import com.bnpp.regliss.six.dto.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@ReglissWebProfile
public class SixFiltersMapper {

    public SixFiltersDto toSixFiltersDto(SixFilters filter) {
        SixFiltersDto filterDto = new SixFiltersDto();
        filterDto.id =  filter.getId();
        filterDto.filterName = filter.getFilterName();
        filterDto.filterTag = filter.getFilterTag();
        filterDto.fileType = filter.getFileType();
        filterDto.isActive = filter.isActive();
        filterDto.deleted = filter.isDeleted();
        filterDto.excludeCmic= filter.isExcludeCMIC();
        filterDto.excludeE014071= filter.isExcludeE014071();
        return filterDto;
    }

    public SixFilters toSixFilter(SixFilters filter, SixFiltersDto filterDto, ReglissList listInDb) {
        filter.setFilterName(filterDto.filterName);
        filter.setFilterTag(filterDto.filterTag);
        filter.setFileType(filterDto.fileType);
        filter.setDeleted(filterDto.deleted);
        filter.setActive(filterDto.isActive);
        filter.setExcludeCMIC(filterDto.excludeCmic);
        filter.setExcludeE014071(filterDto.excludeE014071);
        filter.setList(listInDb);
        return filter;
    }

    public SixFiltersAndSubFiltersDto toSixFilterAndSubfiltersDto(SixFilters filter) {
        SixFiltersAndSubFiltersDto dto = new SixFiltersAndSubFiltersDto();
        dto.sixFilter = toSixFiltersDto(filter);
        dto.sixSubfilters = filter.getSixSubfilters().stream()
                .filter(SixSubFilters::isNotDeleted)
                .sorted(Comparator.comparing(SixSubFilters::getSixIndex))
                .map(this::toSixSubfilterDto)
                .collect(toList());
        return dto;
    }

    public SixSubFiltersDto toSixSubfilterDto(SixSubFilters sixSubFilter) {
        SixSubFiltersDto dto = new SixSubFiltersDto();
        dto.id = sixSubFilter.getId();
        dto.subFilterName = sixSubFilter.getSixSubFilterName();
        dto.sixIndex = sixSubFilter.getSixIndex();
        dto.nestedFilterStatus = sixSubFilter.getNestedFilterStatus();
        dto.deleted = sixSubFilter.getDeleted();
        dto.filters = sixSubFilter.getSixSubORFilters().stream().filter(SixSubORFilters::isNotDeleted).sorted(Comparator.comparing(SixSubORFilters::getSixORIndex))
                .map(this::toSixSubORFilterDto).collect(toList());
        return dto;
    }

    public SixSubFilters toSixSubfilter(SixSubFiltersDto subfilterDto) {
        SixSubFilters subfilter = new SixSubFilters();
        return this.toSixSubfilter(subfilterDto,subfilter);
    }

    public SixSubFilters toSixSubfilter(SixSubFiltersDto subfilterDto, SixSubFilters subfilter) {
        subfilter.setId(subfilterDto.id);
        subfilter.setSixIndex(subfilterDto.sixIndex);
        subfilter.setSixSubFilterName(subfilterDto.subFilterName);
        subfilter.setDeleted(subfilterDto.deleted);
        subfilter.setNestedFilterStatus(subfilterDto.nestedFilterStatus);

        List<SixSubORFiltersDto> sixSubORFiltersdtos = subfilterDto.filters;
        subfilter.updateSixSubORFilters(sixSubORFiltersdtos.stream().map(this::toSixSubORFilters).collect(toList()));

        return subfilter;

    }

    public SixSubFilters toAuditSixSubfilter(SixSubFiltersDto subfilterDto) {
        SixSubFilters subfilter = new SixSubFilters();
        subfilter.setId(subfilterDto.id);
        subfilter.setSixIndex(subfilterDto.sixIndex);
        subfilter.setSixSubFilterName(subfilterDto.subFilterName);
        subfilter.setDeleted(subfilterDto.deleted);

        List<SixSubORFiltersDto> sixSubORFiltersdtos = subfilterDto.filters;
        subfilter.updateAuditSixSubORFilters(sixSubORFiltersdtos.stream().map(this::toSixSubORFilters).collect(toList()));

        return subfilter;

    }

    public SixSubORFiltersDto toSixSubORFilterDto(SixSubORFilters sixSubORFilter) {
        SixSubORFiltersDto dto = new SixSubORFiltersDto();
        dto.id = sixSubORFilter.getId();
        dto.sixORIndex = sixSubORFilter.getSixORIndex();
        dto.fieldName = sixSubORFilter.getFieldName();
        dto.operator = sixSubORFilter.getOperator();
        dto.filterValue = sixSubORFilter.getFilterValue();
        dto.deleted = sixSubORFilter.getDeleted();
        return dto;
    }
    public SixSubORFilters toSixSubORFilters(SixSubORFiltersDto sixSubORFilterdto) {
        SixSubORFilters sixSubORFilters = new SixSubORFilters();
        sixSubORFilters.setId(sixSubORFilterdto.id);
        sixSubORFilters.setSixORIndex(sixSubORFilterdto.sixORIndex);
        sixSubORFilters.setFieldName(sixSubORFilterdto.fieldName);
        sixSubORFilters.setOperator(sixSubORFilterdto.operator);
        sixSubORFilters.setFilterValue(sixSubORFilterdto.filterValue);
        sixSubORFilters.setDeleted(sixSubORFilterdto.deleted);
        return sixSubORFilters;
    }

    public SixTableFieldNamesDto toSixTableFieldNamesDto(SixTableFieldNames sixTableFieldNames) {
        return new SixTableFieldNamesDto(sixTableFieldNames.getFilterType(), sixTableFieldNames.getFieldNames());
    }

}

