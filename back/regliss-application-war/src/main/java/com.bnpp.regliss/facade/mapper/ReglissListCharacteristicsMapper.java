package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.ListCharacteristicsDto;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.facade.dto.list.ListVersionDto;
import com.bnpp.regliss.facade.dto.list.ReglissListCharacteristicsDto;
import com.bnpp.regliss.facade.dto.list.ReglissListDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.vo.ListVersionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Service
@ReglissWebProfile
public class ReglissListCharacteristicsMapper {

    @Autowired
    private ReglissListRepository listRepository;

    @Autowired
    private ReferenceService reference;

    public ReglissListCharacteristicsDto toListCharacteristicsDto(ListCharacteristicsDto list) {
        ReglissListCharacteristicsDto dto = toDto(list);
        return dto;
    }

    public ReglissList toEntity(ReglissList entity, ReglissListCharacteristicsDto dto) {
        entity.setShortName(dto.shortName);
        entity.setName(dto.name);
        entity.setRegulatorOrInternalPolicy(reference.getRegulatorOrInternalPolicyById(dto.regulatorOrInternalPolicy));
        entity.setDescription(dto.description);
        entity.setConfidentiality(dto.confidentiality);
        entity.setCountry(reference.getCountryById(dto.country));
        entity.setListCategory(reference.getCategoryById(dto.listCategory));
        entity.setIssuer(reference.getIssuerById(dto.issuer));
        entity.setOwner(dto.owner);
        entity.setReference(dto.reference);
        entity.setType(dto.type);
        entity.setScope(dto.scope);
        entity.setActive(dto.active);
        entity.setGlobalPlusOfac(dto.isGlobalPlusOfac);
        entity.setGeographicData(dto.geographicData != null ? reference.getGeographicDataById(dto.geographicData) : null);
        return entity;
    }

    private ReglissListCharacteristicsDto toDto(ListCharacteristicsDto list) {
        ReglissListCharacteristicsDto dto = new ReglissListCharacteristicsDto();
        dto.id = list.getId();
        dto.shortName = list.getShortName();
        dto.name = list.getName();
        dto.regulatorOrInternalPolicy = list.getRegulatorOrInternalPolicy().getId();
        dto.description = list.getDescription();
        dto.confidentiality = list.getConfidentiality();
        dto.country = list.getCountry().getId();
        dto.territory = list.getCountry().getId();
        dto.listCategory = list.getListCategory().getId();
        dto.issuer = list.getIssuer().getId();
        dto.owner = list.getOwner();
        dto.reference = list.getReference();
        dto.type = list.getType();
        dto.scope = list.getScope();
        dto.active = list.getActive();
        dto.isGlobalPlusOfac = list.getGlobalPlusOfac();
        dto.geographicData = list.getGeographicData()!=null ? list.getGeographicData().getId(): null;
        return dto;
    }
}
