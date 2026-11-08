package com.bnpp.regliss.facade.dto.record;

import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.referential.record.ProgramSanctionType;
import lombok.Getter;

public class ProgramDto {

    public Long id;
    @Getter public String label;
    @Getter public Long sanctionType;
    @Getter public String description;
    public Long recordCount;

    public ProgramDto(Program program, Long recordCount) {
        this.id = program.getId();
        this.label = program.getLabelOpt().orElse(null);
        this.sanctionType = program.getSanctionTypeOpt().map(ProgramSanctionType::getId).orElse(null);
        this.description = program.getDescription();
        this.recordCount = recordCount;
    }

    public ProgramDto(String label) { this.label = label; }

    public ProgramDto(Long id, String label) {
        this(label);
        this.id = id;
    }

    public ProgramDto() {
    }

}

