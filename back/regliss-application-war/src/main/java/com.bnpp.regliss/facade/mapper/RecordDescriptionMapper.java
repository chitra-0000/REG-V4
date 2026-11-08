package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.referential.record.Description3;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.entity.referential.record.SanctionType;
import com.bnpp.regliss.facade.dto.record.RecordDescriptionDto;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class RecordDescriptionMapper {

    public RecordDescriptionDto recordCategoryToDto(RecordCategory recordCategory) {
        RecordDescriptionDto dto = new RecordDescriptionDto();

        dto.id = recordCategory.getId();
        dto.label = recordCategory.getLabel();
        dto.descriptions = recordCategory.getSanctionTypes().stream()
                .map(sanctionType -> this.sanctionTypeToDto(sanctionType)).collect(Collectors.toList());
        return dto;
    }

    private RecordDescriptionDto sanctionTypeToDto(SanctionType sanctionType) {
        RecordDescriptionDto dto = new RecordDescriptionDto();

        dto.id = sanctionType.getId();
        dto.label = sanctionType.getLabel();
        dto.descriptions = sanctionType.getDescriptions3().stream()
                .map(description3 -> this.description3ToDto(description3)).collect(Collectors.toList());

        return dto;
    }

    private RecordDescriptionDto description3ToDto(Description3 description3) {
        RecordDescriptionDto dto = new RecordDescriptionDto();

        dto.id = description3.getId();
        dto.label = description3.getLabel();

        return dto;
    }
}
