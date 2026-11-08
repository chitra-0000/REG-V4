package com.bnpp.regliss.facade.dto.version;

import com.bnpp.regliss.entity.record.Program;

public class ProgramRef {
    public Long id;
    public String name;

    public ProgramRef(Program program) {
        id = program.getId();
        name = program.getLabelOpt().orElse(null);
    }
}

