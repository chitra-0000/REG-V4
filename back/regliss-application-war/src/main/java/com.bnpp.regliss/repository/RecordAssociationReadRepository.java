package com.bnpp.regliss.repository;


import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.facade.dto.record.RecordAssociationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Repository
public class RecordAssociationReadRepository {

    @Autowired
    private RecordAssociationRepository associationRepo;

    public List<RecordAssociationDto> getRecordAssociations(String uuid) {
        return associationRepo.getWatchlistAssocationsFields(uuid)
                .stream()
                .map(this::mapToAssociationDto)
                .collect(toList());
    }

    private RecordAssociationDto mapToAssociationDto(Object[] association) {
        RecordAssociationDto associationDto = new RecordAssociationDto();
        associationDto.name = (String)association[0];
        associationDto.ex = ((Number)association[1]).intValue() != 0;
        associationDto.type = RecordType.valueOf((String)association[2]);
        associationDto.relationTypeId = ((Number) association[3]).longValue();
        associationDto.categories = asList(((String) association[4]).split(","))
                .stream()
                .map(Long::parseLong)
                .collect(toSet());
        associationDto.uuid=(String)association[5];
        return associationDto;
    }

}
