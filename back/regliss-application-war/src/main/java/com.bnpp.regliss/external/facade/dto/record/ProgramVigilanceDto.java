package com.bnpp.regliss.external.facade.dto.record;

import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.referential.record.ProgramSanctionType;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProgramVigilanceDto {
    @JsonIgnore
    public Long id;

    public String label;
    @JsonIgnore
    public Long sanctionType;
    public String sanctionTypeLabel;
    public String description;

    public ProgramVigilanceDto(Program program) {
        this.label = program.getLabelOpt().orElse(null);
        this.sanctionType = program.getSanctionTypeOpt().map(ProgramSanctionType::getId).orElse(null);
        this.sanctionTypeLabel = program.getSanctionTypeOpt().map(ProgramSanctionType::getLabel).orElse(null);
        this.description = program.getDescription();
    }

    public ProgramVigilanceDto(String label) { this.label = label; }

    public ProgramVigilanceDto(Long id, String label) {
        this(label);
        this.id = id;
    }

    public ProgramVigilanceDto() {
    }

}

